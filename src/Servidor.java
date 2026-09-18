import java.io.*;
import java.net.*;
/**
 * Classe Servidor do sistema SiCA (Sistema de Compartilhamento de Arquivos).
 * Atua como um servidor TCP que recebe e processa requisições de clientes:
 * - LIST: Envia a lista de arquivos locais.
 * - UPLOAD: Recebe e armazena arquivos enviados pelo cliente.
 * - DOWNLOAD: Transfere arquivos solicitados pelo cliente.
 */
public class Servidor {
    private static final int PORTA = 12345;
    private static final String DIRETORIO_SERVIDOR = "./servidor_arquivos";
    public static void main(String[] args) {
        // Garante que a pasta de compartilhamento do servidor exista no sistema
        File pasta = new File(DIRETORIO_SERVIDOR);
        if (!pasta.exists()) {
            pasta.mkdirs();
        }
        System.out.println("=== Servidor SiCA Iniciado na Porta " + PORTA + " ===");
        // Cria o ServerSocket para escutar conexões TCP na porta especificada
        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            while (true) {
                System.out.println("\nAguardando conexão de cliente...");
                
                // accept() bloqueia a execução até que um cliente estabeleça conexão
                Socket socket = serverSocket.accept();
                System.out.println("Cliente conectado: " + socket.getInetAddress().getHostAddress());
                // Processa a requisição recebida
                tratarCliente(socket);
            }
        } catch (IOException e) {
            System.err.println("Erro no Servidor: " + e.getMessage());
        }
    }
    /**
     * Processa os comandos recebidos do cliente e chama o método apropriado.
     */
    private static void tratarCliente(Socket socket) {
        try (
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream())
        ) {
            // Lê o comando enviado pelo cliente
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
     * Método responsável por listar todos os arquivos presentes no servidor e enviar ao cliente.
     */
    private static void listarArquivos(DataOutputStream out) throws IOException {
        File pasta = new File(DIRETORIO_SERVIDOR);
        String[] arquivos = pasta.list();
        if (arquivos == null || arquivos.length == 0) {
            out.writeInt(0); // Informa 0 caso não existam arquivos
            return;
        }
        // 1. Envia a quantidade de arquivos para o cliente saber quantos nomes lerá
        out.writeInt(arquivos.length);
        // 2. Envia o nome de cada arquivo
        for (String nome : arquivos) {
            out.writeUTF(nome);
        }
    }
    /**
     * Método responsável por receber um arquivo enviado pelo cliente (Upload) e salvá-lo no disco.
     */
    private static void receberUpload(DataInputStream in, DataOutputStream out) throws IOException {
        // 1. Lê o nome e o tamanho total do arquivo enviado pelo cliente
        String nomeArquivo = in.readUTF();
        long tamanhoArquivo = in.readLong();
        File arquivoDestino = new File(DIRETORIO_SERVIDOR, nomeArquivo);
        // 2. Grava os bytes recebidos via Socket no arquivo em disco
        try (FileOutputStream fos = new FileOutputStream(arquivoDestino)) {
            byte[] buffer = new byte[4096]; // Buffer de 4KB
            long totalLido = 0;
            int bytesLidos;
            while (totalLido < tamanhoArquivo && 
                   (bytesLidos = in.read(buffer, 0, (int) Math.min(buffer.length, tamanhoArquivo - totalLido))) != -1) {
                fos.write(buffer, 0, bytesLidos);
                totalLido += bytesLidos;
            }
        }
        System.out.println("Upload concluído com sucesso: " + nomeArquivo + " (" + tamanhoArquivo + " bytes)");
        out.writeUTF("OK: Arquivo enviado com sucesso!");
    }
    /**
     * Método responsável por ler um arquivo do disco e enviá-lo ao cliente (Download).
     */
    private static void enviarDownload(DataInputStream in, DataOutputStream out) throws IOException {
        // 1. Lê o nome do arquivo que o cliente deseja baixar
        String nomeArquivo = in.readUTF();
        File arquivo = new File(DIRETORIO_SERVIDOR, nomeArquivo);
        // 2. Verifica se o arquivo existe e avisa o cliente
        if (!arquivo.exists() || arquivo.isDirectory()) {
            out.writeBoolean(false); // Arquivo não encontrado
            return;
        }
        out.writeBoolean(true); // Arquivo encontrado
        out.writeLong(arquivo.length()); // Envia o tamanho do arquivo
        // 3. Lê o arquivo em blocos de bytes e envia pelo Socket
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