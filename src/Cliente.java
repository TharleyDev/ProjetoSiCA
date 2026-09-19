import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Cliente do SiCA. Permite listar, enviar e baixar arquivos
 * por meio de uma conexão TCP com o servidor.
 */
public class Cliente {

    private static final String IP_SERVIDOR = "127.0.0.1";
    private static final int PORTA_SERVIDOR = 12345;
    private static final String DIRETORIO_CLIENTE = "./cliente_arquivos";

    /**
     * Inicia o cliente, cria a pasta local caso necessário
     * e exibe o menu de opções para o usuário.
     */
    public static void main(String[] args) {
        File pasta = new File(DIRETORIO_CLIENTE);
        if (!pasta.exists()) {
            pasta.mkdirs();
        }

        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Cliente SiCA Iniciado ===");

        while (true) {
            System.out.println("\n--- MENU SICA ---");
            System.out.println("1 - Listar arquivos no servidor");
            System.out.println("2 - Enviar arquivo para o servidor (Upload)");
            System.out.println("3 - Baixar arquivo do servidor (Download)");
            System.out.println("0 - Sair");
            System.out.print("Escolha uma opção: ");

            int opcao = scanner.nextInt();
            scanner.nextLine();

            if (opcao == 0) {
                System.out.println("Encerrando aplicação...");
                break;
            }

            switch (opcao) {
                case 1:
                    solicitarListagem();
                    break;
                case 2:
                    realizarUpload(scanner);
                    break;
                case 3:
                    realizarDownload(scanner);
                    break;
                default:
                    System.out.println("Opção inválida!");
            }
        }

        scanner.close();
    }

    /**
     * Solicita ao servidor a lista de arquivos disponíveis
     * e exibe os nomes recebidos no terminal.
     */
    private static void solicitarListagem() {
        try (
            Socket socket = new Socket(IP_SERVIDOR, PORTA_SERVIDOR);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream())
        ) {
            out.writeUTF("LIST");

            int quantidade = in.readInt();

            if (quantidade == 0) {
                System.out.println("Nenhum arquivo disponível no servidor.");
            } else {
                System.out.println("\n--- Arquivos no Servidor (" + quantidade + ") ---");

                for (int i = 0; i < quantidade; i++) {
                    System.out.println("- " + in.readUTF());
                }
            }
        } catch (IOException e) {
            System.err.println("Erro na comunicação com o servidor: " + e.getMessage());
        }
    }

    /**
     * Envia um arquivo da pasta local do cliente para o servidor.
     */
    private static void realizarUpload(Scanner scanner) {
        System.out.print("Digite o nome do arquivo presente em '" + DIRETORIO_CLIENTE + "': ");
        String nomeArquivo = scanner.nextLine();

        File arquivo = new File(DIRETORIO_CLIENTE, nomeArquivo);

        if (!arquivo.exists()) {
            System.out.println("ERRO: O arquivo '" + nomeArquivo + "' não foi encontrado na pasta local do cliente.");
            return;
        }

        try (
            Socket socket = new Socket(IP_SERVIDOR, PORTA_SERVIDOR);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream())
        ) {
            out.writeUTF("UPLOAD");
            out.writeUTF(arquivo.getName());
            out.writeLong(arquivo.length());

            // Envia o conteúdo do arquivo em blocos de bytes.
            try (FileInputStream fis = new FileInputStream(arquivo)) {
                byte[] buffer = new byte[4096];
                int bytesLidos;

                while ((bytesLidos = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesLidos);
                }
            }

            String resposta = in.readUTF();
            System.out.println("Servidor: " + resposta);

        } catch (IOException e) {
            System.err.println("Erro ao realizar upload: " + e.getMessage());
        }
    }

    /**
     * Solicita um arquivo ao servidor e salva o conteúdo recebido
     * na pasta local do cliente.
     */
    private static void realizarDownload(Scanner scanner) {
        System.out.print("Digite o nome do arquivo que deseja baixar do servidor: ");
        String nomeArquivo = scanner.nextLine();

        try (
            Socket socket = new Socket(IP_SERVIDOR, PORTA_SERVIDOR);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream())
        ) {
            out.writeUTF("DOWNLOAD");
            out.writeUTF(nomeArquivo);

            boolean existe = in.readBoolean();
            if (!existe) {
                System.out.println("ERRO: O arquivo informado não foi encontrado no servidor.");
                return;
            }

            long tamanhoArquivo = in.readLong();
            File arquivoDestino = new File(DIRETORIO_CLIENTE, nomeArquivo);

            // Recebe o arquivo em blocos até completar o tamanho informado.
            try (FileOutputStream fos = new FileOutputStream(arquivoDestino)) {
                byte[] buffer = new byte[4096];
                long totalLido = 0;
                int bytesLidos;

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

            System.out.println("Download do arquivo '" + nomeArquivo + "' concluído com sucesso!");

        } catch (IOException e) {
            System.err.println("Erro ao realizar download: " + e.getMessage());
        }
    }
}
