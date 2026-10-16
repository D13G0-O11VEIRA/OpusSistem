package br.opussistem.service;

import br.opussistem.dao.UsuarioDAO;
import br.opussistem.exception.AppException;
import br.opussistem.model.PerfilUsuario;
import br.opussistem.model.Usuario;
import br.opussistem.util.Armazenamento;
import br.opussistem.util.Senhas;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** Cadastro, login e perfil de usuários. */
public class AuthService {

    private static final Pattern LOGIN_VALIDO = Pattern.compile("[a-z0-9._-]{3,30}");
    static final int SENHA_MINIMA = 6;

    private final UsuarioDAO dao = new UsuarioDAO();

    /** O primeiro usuário cadastrado vira administrador; os demais usam o perfil informado (padrão: operador). */
    public Usuario cadastrar(String nome, String usuario, String email, String senha, String confirmacao,
                             PerfilUsuario perfilSolicitado) {
        String login = usuario == null ? "" : usuario.trim().toLowerCase();
        if (nome == null || nome.isBlank() || nome.trim().length() < 2) {
            throw new AppException("Informe o nome completo.");
        }
        if (!LOGIN_VALIDO.matcher(login).matches()) {
            throw new AppException("O usuário deve ter de 3 a 30 caracteres (letras minúsculas, números, ponto, hífen ou _).");
        }
        validarSenha(senha, confirmacao);
        if (dao.buscarPorUsuario(login).isPresent()) {
            throw new AppException("Esse usuário já existe.");
        }
        Usuario u = new Usuario();
        u.setNome(nome.trim());
        u.setUsuario(login);
        u.setEmail(email == null || email.isBlank() ? null : email.trim());
        u.setSenhaHash(Senhas.gerarHash(senha));
        boolean primeiro = dao.contar() == 0;
        u.setPerfil(primeiro ? PerfilUsuario.ADMIN : (perfilSolicitado == null ? PerfilUsuario.OPERADOR : perfilSolicitado));
        u.setId(dao.inserir(u));
        return u;
    }

    public Usuario entrar(String usuario, String senha) {
        String login = usuario == null ? "" : usuario.trim().toLowerCase();
        Optional<Usuario> u = dao.buscarPorUsuario(login);
        if (u.isEmpty() || !u.get().isAtivo() || !Senhas.conferir(senha, u.get().getSenhaHash())) {
            throw new AppException("Usuário ou senha incorretos.");
        }
        return u.get();
    }

    public boolean existemUsuarios() {
        return dao.contar() > 0;
    }

    public List<Usuario> listar() {
        return dao.listar();
    }

    /** Atualiza nome, e-mail e cargo do próprio usuário; troca a senha se informada. */
    public void atualizarPerfil(Usuario u, String novaSenha) {
        if (u.getNome() == null || u.getNome().isBlank()) {
            throw new AppException("O nome não pode ficar vazio.");
        }
        dao.atualizarDados(u);
        if (novaSenha != null && !novaSenha.isEmpty()) {
            validarSenha(novaSenha, novaSenha);
            u.setSenhaHash(Senhas.gerarHash(novaSenha));
            dao.atualizarSenha(u.getId(), u.getSenhaHash());
        }
    }

    public void excluir(long id, Usuario solicitante) {
        if (!solicitante.isAdmin()) {
            throw new AppException("Apenas administradores podem excluir usuários.");
        }
        if (solicitante.getId() == id) {
            throw new AppException("Você não pode excluir o próprio usuário.");
        }
        dao.excluir(id);
    }

    // ---- assinatura salva do usuário (para assinar como técnico com um clique)

    public void salvarAssinatura(Usuario u, byte[] png) {
        String nome = "usuario-" + u.getId() + ".png";
        Armazenamento.gravar(Armazenamento.assinaturas().resolve(nome), png);
        u.setAssinaturaArquivo(nome);
        dao.atualizarAssinatura(u.getId(), nome);
    }

    public Optional<byte[]> lerAssinatura(Usuario u) {
        if (u.getAssinaturaArquivo() == null) {
            return Optional.empty();
        }
        Path arquivo = Armazenamento.assinaturas().resolve(u.getAssinaturaArquivo());
        return Files.exists(arquivo) ? Optional.of(Armazenamento.ler(arquivo)) : Optional.empty();
    }

    public void removerAssinatura(Usuario u) {
        if (u.getAssinaturaArquivo() != null) {
            Armazenamento.apagarSilencioso(Armazenamento.assinaturas().resolve(u.getAssinaturaArquivo()));
        }
        u.setAssinaturaArquivo(null);
        dao.atualizarAssinatura(u.getId(), null);
    }

    private void validarSenha(String senha, String confirmacao) {
        if (senha == null || senha.length() < SENHA_MINIMA) {
            throw new AppException("A senha precisa ter ao menos " + SENHA_MINIMA + " caracteres.");
        }
        if (!senha.equals(confirmacao)) {
            throw new AppException("As senhas não conferem.");
        }
    }
}
