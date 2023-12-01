import java.util.Arrays;
import java.util.Scanner;

public class Terminal {
    private FileSystem fileSystem;

    public Terminal() {
        fileSystem = new FileSystem(65536);
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        boolean loggedIn = false;

        while (fileSystem.isSystemRunning()) {
            if (!loggedIn) {
                System.out.print("\u001B[38;5;15mTo log in enter '");
                System.out.print("\u001B[94mlogin \u001B[92m{username} {password}\u001B[38;5;15m' or '\u001B[94mexit\u001B[38;5;15m' to quit the host.\n@localhost: ");
            } else {
                System.out.print(fileSystem.currentUsername + "@localhost:" + fileSystem.currentDirectory + "# ");
            }

            String userInput = scanner.nextLine().trim();
            String[] inputParts = userInput.split(" ");
            String inputCommand = inputParts.length > 0 ? inputParts[0] : null;
            String[] commandArgs = inputParts.length > 1 ? Arrays.copyOfRange(inputParts, 1, inputParts.length) : new String[0];

            if (!loggedIn && !"login".equals(inputCommand) && !"exit".equals(inputCommand)) {
                System.out.println("Please log in to access the system.");
                continue;
            }

            switch (inputCommand) {
                case "exit":
                    fileSystem.exit();
                    break;
                case "login":
                    if (fileSystem.currentUserId == null) {
                        if (checkArgs(commandArgs, 2)) {
                            if (!fileSystem.login(commandArgs[0], commandArgs[1])) {
                                System.out.println();
                            } else {
                                loggedIn = true;
                            }
                        }
                    }
                    break;
                case "format":
                    fileSystem.format();
                    break;
                case "touch":
                    if (checkArgs(commandArgs, 1)) {
                        fileSystem.touch(commandArgs[0]);
                    }
                    break;
                case "write":
                    if (checkArgs(commandArgs, 2)) {
                        fileSystem.writeContent(commandArgs[0], commandArgs[1]);
                    }
                    break;
                case "cat":
                    if (checkArgs(commandArgs, 1)) {
                        fileSystem.cat(commandArgs[0]);
                    }
                    break;
                case "chmod":
                    if (checkArgs(commandArgs, 2)) {
                        fileSystem.chmod(commandArgs[0], commandArgs[1]);
                    }
                    break;
                case "mkdir":
                    if (checkArgs(commandArgs, 1)) {
                        fileSystem.mkdir(commandArgs[0]);
                    }
                    break;
                case "rmdir":
                    if (checkArgs(commandArgs, 1)) {
                        fileSystem.rmdir(commandArgs[0]);
                    }
                    break;
                case "ls":
                    fileSystem.ls();
                    break;
                case "adduser":
                    if (checkArgs(commandArgs, 3)) {
                        boolean isAdmin = commandArgs.length > 3 ? Boolean.parseBoolean(commandArgs[3]) : false;
                        fileSystem.adduser(commandArgs[0], commandArgs[1], commandArgs[2], isAdmin);
                    }
                    break;
                case "rmuser":
                    if (checkArgs(commandArgs, 1)) {
                        fileSystem.rmuser(commandArgs[0]);
                    }
                    break;
                case "lsuser":
                    fileSystem.lsuser();
                    break;
                case "logout":
                    fileSystem.logout();
                    loggedIn = false;
                    break;
                case "stat":
                    if (checkArgs(commandArgs, 1)) {
                        fileSystem.stat(commandArgs[0]);
                    }
                    break;
                case "rm":
                    if (checkArgs(commandArgs, 1)) {
                        fileSystem.rm(commandArgs[0]);
                    }
                    break;
                case "chown":
                    if (checkArgs(commandArgs, 2)) {
                        fileSystem.chown(commandArgs[0], commandArgs[1]);
                    }
                    break;
                case "cd":
                    if (checkArgs(commandArgs, 1)) {
                        fileSystem.cd(commandArgs[0]);
                    }
                    break;
                case "sudo":
                    if (fileSystem.currentUserId == null) {
                        System.out.println("Login required for 'sudo' command.");
                    } else {
                        if (commandArgs.length >= 1 && "su".equals(commandArgs[0])) {
                            fileSystem.sudoSu();
                        } else {
                            System.out.println("Invalid argument for 'sudo' command. Expected 'su'.");

                        }
                    }
                    break;

                case "help":
                    fileSystem.help();
                    break;
                case "":
                    continue;
                default:
                    if (fileSystem.currentUserId != null) {
                        System.out.println("Command not recognized");
                    }
            }
        }
    }

    private boolean checkArgs(String[] args, int expected) {
        if (args.length < expected) {
            System.out.println("Please provide " + expected + " argument(s).");
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        Terminal terminal = new Terminal();
        terminal.run();
    }
}
