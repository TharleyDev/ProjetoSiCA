import java.io.*;
import java.net.*;

/**
 * Servidor do SiCA. Recebe conexões TCP e processa os comandos
 * LIST, UPLOAD e DOWNLOAD enviados pelo cliente.
 */
public class Servidor {

    private static final int PORTA = 12345;
    private static final String DIRETORIO_SERVIDOR = "./servidor_arquivos";

    /**
     * Inicia o servidor, cria a pasta de arquivos caso necessário
     * e permanece aguardando conexões de clientes.
     */
    public static void main(String[] args) {
        File pasta = new File(DIRETORIO_SERVIDOR);
        if (!pasta.exists()) {
            pasta.mkdirs();
        }

        System.out.println("=== Servidor SiCA Iniciado na Porta " + PORTA + " ===");

        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            while (true) {
                System.out.println("\nAguardando conexão de cliente...");
                Socket socket = serverSocket.accept();
                System.out.println("Cliente conectado: " + socket.getInetAddress().getHostAddress());

                tratarCliente(socket);
            }
        } catch (IOException e) {
            System.err.println("Erro no Servidor: " + e.getMessage());
        }
    }

    /**
     * Lê o comando enviado pelo cliente e direciona a requisição
     * para a operação correspondente.
     */
    private static void tratarCliente(Socket socket) {
        try (
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream())
        ) {
            String comando = in.readUTF();

            switch (comando.toUpperCase()) {
                case "LIST":
                    listarArquivos(out);
                    break;
                case "UPLOAD":
                    receberUpload(in, out);
                    break;
                case "DOWNLOAD":
                    enviarDownload(in, out);
                    break;
                default:
                    out.writeUTF("ERRO: Comando inválido.");
            }
        } catch (IOException e) {
            System.err.println("Erro ao tratar requisição do cliente: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar socket: " + e.getMessage());
            }
        }
    }

    /**
     * Envia ao cliente a quantidade de arquivos disponíveis
     * e, em seguida, o nome de cada um deles.
     */
    private static void listarArquivos(DataOutputStream out) throws IOException {
        File pasta = new File(DIRETORIO_SERVIDOR);
        String[] arquivos = pasta.list();

        if (arquivos == null || arquivos.length == 0) {
            out.writeInt(0);
            return;
        }

        out.writeInt(arquivos.length);
        for (String nome : arquivos) {
            out.writeUTF(nome);
        }
    }

    /**
     * Recebe um arquivo enviado pelo cliente e o salva
     * no diretório de arquivos do servidor.
     */
    private static void receberUpload(DataInputStream in, DataOutputStream out) throws IOException {
        String nomeArquivo = in.readUTF();
        long tamanhoArquivo = in.readLong();

        File arquivoDestino = new File(DIRETORIO_SERVIDOR, nomeArquivo);

        try (FileOutputStream fos = new FileOutputStream(arquivoDestino)) {
            byte[] buffer = new byte[4096];
            long totalLido = 0;
            int bytesLidos;

            // Recebe o arquivo em blocos até completar o tamanho informado.
            while (totalLido < tamanhoArquivo &&
                   (bytesLidos = in.read(
                       buffer,
                       0,
                       (int) Math.min(buffer.length, tamanhoArquivo - totalLido)
                   )) != -1) {
                fos.write(buffer, 0, bytesLidos);
                totalLido += bytesLidos;
            }
        }

        System.out.println("Upload concluído com sucesso: " + nomeArquivo + " (" + tamanhoArquivo + " bytes)");
        out.writeUTF("OK: Arquivo enviado com sucesso!");
    }

    /**
     * Verifica se o arquivo solicitado existe e, caso exista,
     * envia seu tamanho e conteúdo ao cliente.
     */
    private static void enviarDownload(DataInputStream in, DataOutputStream out) throws IOException {
        String nomeArquivo = in.readUTF();
        File arquivo = new File(DIRETORIO_SERVIDOR, nomeArquivo);

        if (!arquivo.exists() || arquivo.isDirectory()) {
            out.writeBoolean(false);
            return;
        }

        out.writeBoolean(true);
        out.writeLong(arquivo.length());

        try (FileInputStream fis = new FileInputStream(arquivo)) {
            byte[] buffer = new byte[4096];
            int bytesLidos;

            while ((bytesLidos = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesLidos);
            }
        }

        System.out.println("Download enviado ao cliente com sucesso: " + nomeArquivo);
    }
}
