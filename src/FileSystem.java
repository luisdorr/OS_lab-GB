import java.time.LocalDateTime;
import java.util.*;

class FileSystem {
    int maxBlocks;
    int blocks;
    int blockSize;
    Map<String, Inode> inodes;
    String rootDirectoryPath;
    String currentDirectory;
    List<User> users;
    String currentUserId;
    String currentUsername;

    //Attributes para usar com o sudo e exit
     String previousUserId;
     String previousUsername;
    private boolean systemRunning;

    public FileSystem(int maxBlocks) {
        this.maxBlocks = maxBlocks;
        this.blocks = 0;
        this.blockSize = 512;
        this.inodes = new HashMap<>();
        this.rootDirectoryPath = "/";
        this.currentDirectory = this.rootDirectoryPath;
        this.users = new ArrayList<>();
        this.currentUserId = null;
        this.currentUsername = null;
        this.adduser("root", "1234", "0", true);
        this.adduser("user", "4321", "1", false);
        this.mkdir(this.rootDirectoryPath);
        this.systemRunning = true;
    }

    public void format() {
        this.maxBlocks = maxBlocks;
        this.blocks = 0;
        this.blockSize = 512;
        this.inodes = new HashMap<>();
        this.rootDirectoryPath = "/";
        this.currentDirectory = this.rootDirectoryPath;
        this.users = new ArrayList<>();
        this.currentUserId = null;
        this.currentUsername = null;
        this.adduser("root", "1234", "0", true);
        this.adduser("user", "4321", "1", false);
        this.mkdir(this.rootDirectoryPath);
        this.systemRunning = true;
    }

    public void touch(String fileName) {
        String filePath = this.getFileOrDirectoryPath(fileName);
        if (this.inodes.containsKey(filePath)) {
            Inode inode = this.inodes.get(filePath);
            inode.lastAccessedDate = LocalDateTime.now();
            inode.lastModifiedDate = LocalDateTime.now();
        } else {
            Inode newInode = new Inode(this.currentUserId, false);
            this.inodes.put(filePath, newInode);
        }
    }

    public void writeContent(String fileName, String data) {
        String filePath = this.getFileOrDirectoryPath(fileName);
        if (this.inodes.containsKey(filePath)) {
            Inode inode = this.inodes.get(filePath);
            if (inode.isDirectory) {
                System.out.println(fileName + " Is a directory.");
                return;
            }
            if (!this.hasWritePermission(inode)) {
                System.out.println("User does not have permission to write file: " + fileName);
                return;
            }

            byte[] bytesData = data.getBytes();
            int bytesCount = bytesData.length;

            inode.blocks = new ArrayList<>();
            inode.size = bytesCount;
            this.blocks -= inode.blockCount;
            inode.blockCount = 0;

            for (int byteIndex = 0; byteIndex < bytesCount; byteIndex += this.blockSize) {
                inode.blockCount += 1;
                if (inode.blockCount > 10) {
                    System.out.println("Content exceeds 10 blocks of " + this.blockSize + " bytes each.");
                    inode.blockCount = 0;
                    inode.blocks = new ArrayList<>();
                    break;
                }
                Block newBlock = new Block(this.blockSize);
                List<Byte> blockContent = new ArrayList<>();
                for (int i = byteIndex; i < byteIndex + this.blockSize && i < bytesCount; i++) {
                    blockContent.add(bytesData[i]);
                }
                newBlock.write(blockContent);
                inode.blocks.add(newBlock);
            }
            this.blocks += inode.blockCount;
            inode.lastModifiedDate = LocalDateTime.now();
        } else {
            System.out.println("File not found.");
        }
    }

    public boolean hasWritePermission(Inode inode) {
        User currentUser = this.getUser(this.currentUsername);
        if (currentUser.isAdmin) {
            return true;
        }
        Map<String, Boolean> inodePermissions = inode.permissions.get("user");
        if (this.currentUserId.equals(inode.ownerId)) {
            if (inodePermissions.get("write")) {
                return true;
            }
        } else if (inode.permissions.get("other").get("write")) {
            return true;
        }
        return false;
    }

    public void cat(String fileName) {
        String filePath = this.getFileOrDirectoryPath(fileName);
        if (this.inodes.containsKey(filePath)) {
            Inode inode = this.inodes.get(filePath);
            if (inode.isDirectory) {
                System.out.println(fileName + " Is a directory.");
                return;
            }
            if (!this.hasReadPermission(inode)) {
                System.out.println("User does not have permission to read file: " + fileName);
                return;
            }
            StringBuilder fileContent = new StringBuilder();
            for (Block block : inode.blocks) {
                if (block != null) {
                    List<Byte> blockContent = block.read();
                    for (byte b : blockContent) {
                        fileContent.append((char) b);
                    }
                }
            }
            System.out.println(fileContent.toString());
        } else {
            System.out.println("File not found.");
        }
    }

    public boolean hasReadPermission(Inode inode) {
        User currentUser = this.getUser(this.currentUsername);
        if (currentUser.isAdmin) {
            return true;
        }
        Map<String, Boolean> inodePermissions = inode.permissions.get("user");
        if (this.currentUserId.equals(inode.ownerId)) {
            if (inodePermissions.get("read")) {
                return true;
            }
        } else if (inode.permissions.get("other").get("read")) {
            return true;
        }
        return false;
    }

    public void rm(String fileName) {
        String filePath = this.getFileOrDirectoryPath(fileName);
        if (this.inodes.containsKey(filePath)) {
            if (!this.inodes.get(filePath).isDirectory) {
                this.inodes.remove(filePath);
            } else {
                System.out.println("Failed to remove '" + fileName + "': Not a file.");
            }
        } else {
            System.out.println("No such file or directory.");
        }
    }

    public void chown(String newOwnerName, String fileDirectoryName) {
        User newOwner = this.getUser(newOwnerName);
        if (newOwner == null) {
            System.out.println("User with specified username not found.");
            return;
        }
        String fileDirectoryPath = this.getFileOrDirectoryPath(fileDirectoryName);
        if (this.inodes.containsKey(fileDirectoryPath)) {
            Inode inode = this.inodes.get(fileDirectoryPath);
            inode.ownerId = newOwner.userId;
        } else {
            System.out.println("No such file or directory.");
        }
    }

    public User getUser(String username) {
        for (User user : this.users) {
            if (user.username.equals(username)) {
                return user;
            }
        }
        return null;
    }

    public void chmod(String permissions, String fileDirectoryName) {
        String fileDirectoryPath = this.getFileOrDirectoryPath(fileDirectoryName);
        if (this.inodes.containsKey(fileDirectoryPath)) {
            Map<String, Map<String, Boolean>> permissionsDictionary = new HashMap<>();
            permissionsDictionary.put("0", new HashMap<>());
            permissionsDictionary.put("1", new HashMap<>());
            permissionsDictionary.put("2", new HashMap<>());
            permissionsDictionary.put("3", new HashMap<>());
            permissionsDictionary.put("4", new HashMap<>());
            permissionsDictionary.put("5", new HashMap<>());
            permissionsDictionary.put("6", new HashMap<>());
            permissionsDictionary.put("7", new HashMap<>());
            permissionsDictionary.get("0").put("read", false);
            permissionsDictionary.get("0").put("write", false);
            permissionsDictionary.get("0").put("execute", false);
            permissionsDictionary.get("1").put("read", false);
            permissionsDictionary.get("1").put("write", false);
            permissionsDictionary.get("1").put("execute", true);
            permissionsDictionary.get("2").put("read", false);
            permissionsDictionary.get("2").put("write", true);
            permissionsDictionary.get("2").put("execute", false);
            permissionsDictionary.get("3").put("read", false);
            permissionsDictionary.get("3").put("write", true);
            permissionsDictionary.get("3").put("execute", true);
            permissionsDictionary.get("4").put("read", true);
            permissionsDictionary.get("4").put("write", false);
            permissionsDictionary.get("4").put("execute", false);
            permissionsDictionary.get("5").put("read", true);
            permissionsDictionary.get("5").put("write", false);
            permissionsDictionary.get("5").put("execute", true);
            permissionsDictionary.get("6").put("read", true);
            permissionsDictionary.get("6").put("write", true);
            permissionsDictionary.get("6").put("execute", false);
            permissionsDictionary.get("7").put("read", true);
            permissionsDictionary.get("7").put("write", true);
            permissionsDictionary.get("7").put("execute", true);

            char[] permissionsChars = permissions.toCharArray();

            for (int i = permissionsChars.length; i < 3; i++) {
                permissionsChars[i] = '0';
            }

            Map<String, Boolean> permissionUser = permissionsDictionary.get(Character.toString(permissionsChars[0]));
            Map<String, Boolean> permissionGroup = permissionsDictionary.get(Character.toString(permissionsChars[1]));
            Map<String, Boolean> permissionOther = permissionsDictionary.get(Character.toString(permissionsChars[2]));
            this.inodes.get(fileDirectoryPath).permissions.put("user", permissionUser);
            this.inodes.get(fileDirectoryPath).permissions.put("group", permissionGroup);
            this.inodes.get(fileDirectoryPath).permissions.put("other", permissionOther);
        } else {
            System.out.println("No such file or directory.");
        }
    }

    public void mkdir(String directoryName) {
        String directoryPath = this.getFileOrDirectoryPath(directoryName);
        if (this.inodes.containsKey(directoryPath)) {
            if (this.inodes.get(directoryPath).isDirectory) {
                System.out.println("Failed to create directory '" + directoryName + "': Directory already exists.");
                return;
            }
        }
        Inode newInode = this.createInode();
        newInode.blocks.add(new Block(this.blockSize));
        newInode.blockCount += 1;
        this.blocks += 1;
        this.inodes.put(directoryPath, newInode);
    }

    public Inode createInode() {
        if (this.currentUserId != null) {
            return new Inode(this.currentUserId, true);
        } else {
            return new Inode("0", true);
        }
    }

    public void rmdir(String directoryName) {
        String directoryPath = this.getFileOrDirectoryPath(directoryName);
        if (this.inodes.containsKey(directoryPath)) {
            Inode inode = this.inodes.get(directoryPath);
            if (inode.isDirectory) {
                if (this.isEmpty(directoryPath)) {
                    this.blocks -= inode.blockCount;
                    this.inodes.remove(directoryPath);
                } else {
                    System.out.println("Failed to remove '" + directoryName + "': Directory not empty.");
                }
            } else {
                System.out.println("Failed to remove '" + directoryName + "': Not a directory.");
            }
        } else {
            System.out.println("No such file or directory.");
        }
    }

    public boolean isEmpty(String directoryPath) {
        for (Map.Entry<String, Inode> entry : this.inodes.entrySet()) {
            String path = entry.getKey();
            Inode inode = entry.getValue();
            if (path.startsWith(directoryPath + "/")) {
                return false;
            }
        }
        return true;
    }

    public void cd(String changeToDirectoryName) {
        String directoryPath = this.getFileOrDirectoryPath(changeToDirectoryName);
        if (changeToDirectoryName.equals("..")) {
            if (!this.currentDirectory.equals("/")) {
                String[] directory = this.currentDirectory.split("/");
                if (directory.length > 2) {
                    this.currentDirectory = String.join("/", Arrays.copyOfRange(directory, 0, directory.length - 1));
                } else {
                    this.currentDirectory = "/";
                }
            } else {
                this.currentDirectory = "/";
            }
        } else if (this.inodes.containsKey(directoryPath)) {
            if (this.inodes.get(directoryPath).isDirectory) {
                this.currentDirectory = this.addSlash(this.currentDirectory) + changeToDirectoryName; // usei o método auxiliar aqui
            } else {
                System.out.println(changeToDirectoryName + "is not a directory.");
            }
        } else {
            System.out.println("No such file or directory.");
        }
    }

    // método auxiliar que adiciona uma "/" no final do diretório se ele não tiver
    private String addSlash(String directory) {
        if (directory.endsWith("/")) {
            return directory;
        } else {
            return directory + "/";
        }
    }


    public void ls() {
        String currentDirectoryPath = this.currentDirectory.equals("/") ? "" : this.currentDirectory;

        for (Map.Entry<String, Inode> entry : this.inodes.entrySet()) {
            String directoryFilePath = entry.getKey();
            Inode inode = entry.getValue();

            if (directoryFilePath.startsWith(currentDirectoryPath) && !directoryFilePath.equals(currentDirectoryPath)) {
                String[] parts = directoryFilePath.substring(currentDirectoryPath.length() + 1).split("/");
                String itemName = parts.length > 0 ? parts[0] : "";

                if (itemName.isEmpty() || !itemName.contains("/")) {
                    if (inode.isDirectory) {
                        System.out.print("\u001B[32m" + itemName + " "); // mudei a cor para verde
                        System.out.print("\u001B[0m");
                    } else {
                        System.out.print(itemName + " ");
                    }
                }
            }
        }
        System.out.println();
    }


    public void adduser(String username, String password, String userId, boolean isAdmin) {
        User currentUser = this.getUser(this.currentUsername);
        if (currentUser != null && !currentUser.isAdmin) {
            System.out.println("Only administrator can add a new user");
            return;
        }
        for (User user : this.users) {
            if (user.username.equals(username)) {
                System.out.println("User with this username already exists.");
                return;
            }
            if (user.userId.equals(userId)) {
                System.out.println("User with this id already exists.");
                return;
            }
        }
        User newUser = new User(username, userId, password, isAdmin);
        this.users.add(newUser);

        if (!userId.equals("0")) {
            System.out.println("User created with success.");
        }
    }

    public void rmuser(String username) {
        User currentUser = this.getUser(this.currentUsername);
        if (currentUser != null && !currentUser.isAdmin) {
            System.out.println("Only administrator can remove a user");
            return;
        }
        if (username.equals("root")) {
            System.out.println("Root user can not be removed.");
            return;
        }
        for (User user : this.users) {
            if (user.username.equals(username)) {
                this.users.remove(user);
                System.out.println("User removed with success.");
                return;
            }
        }
        System.out.println("User with specified username not found.");
    }

    public void lsuser() {
        for (User user : this.users) {
            System.out.println(user.username + " Uid: " + user.userId);
        }
    }

    public boolean login(String username, String password) {
        for (User user : this.users) {
            if (user.username.equals(username)) {
                if (user.password.equals(password)) {
                    this.currentUserId = user.userId;
                    this.currentUsername = user.username;
                    return true;
                } else {
                    System.out.println("Wrong password.");
                    return false;
                }

            }
        }
        System.out.println("User with specified username does not exist.");
        return false;
    }

    public void logout() {
        this.currentUserId = null;
        this.currentUsername = null;
    }

    public void stat(String fileName) {
        String filePath = this.getFileOrDirectoryPath(fileName);
        if (this.inodes.containsKey(filePath)) {
            System.out.println("  File: " + fileName);
            System.out.println(this.inodes.get(filePath).inodeInfo());
        } else {
            System.out.println("No such file or directory.");
        }
    }

    public String getFileOrDirectoryPath(String directoryFileName) {
        if (directoryFileName.equals("/")) {
            return directoryFileName;
        }
        if (this.currentDirectory.equals("/")) {
            return this.currentDirectory + directoryFileName;
        } else {
            return this.currentDirectory + "/" + directoryFileName;
        }
    }

    public void sudoSu() {
        if (!currentUserId.equals("root")) {
            previousUserId = currentUserId;
            previousUsername = currentUsername;
            currentUserId = "root";
            currentUsername = "root";
        } else {
            System.out.println("Already in root mode.");
        }
    }

    public void exit() {
         if (currentUserId != null && currentUserId.equals("root") && previousUserId != null) {
            currentUserId = previousUserId;
            currentUsername = previousUsername;
            previousUserId = null;
            previousUsername = null;
        } else {
            systemRunning = false;
            System.out.println("Exiting the host!");
        }
    }

    public boolean isSystemRunning() {
        return systemRunning;
    }

    public void help() {
        System.out.println("\nAvailable commands:");
        System.out.println("\u001B[38;5;216m- exit\u001B[32m\u001B[0m: Exit the system.");
        System.out.println("\u001B[38;5;216m- format\u001B[32m\u001B[0m: Format the file system.");
        System.out.println("\u001B[38;5;216m- touch \u001B[32m<filename>\u001B[0m: Create a new file.");
        System.out.println("\u001B[38;5;216m- write \u001B[32m<filename> <data>\u001B[0m: Write data to an existing file.");
        System.out.println("\u001B[38;5;216m- cat \u001B[32m<filename>\u001B[0m: Display the contents of a file.");
        System.out.println("\u001B[38;5;216m- chmod \u001B[32m<permissions> <filename>\u001B[0m: Change file permissions.");
        System.out.println("\u001B[38;5;216m- mkdir \u001B[32m<directoryname>\u001B[0m: Create a new directory.");
        System.out.println("\u001B[38;5;216m- rmdir \u001B[32m<directoryname>\u001B[0m: Remove an empty directory.");
        System.out.println("\u001B[38;5;216m- ls\u001B[32m\u001B[0m: List files and directories.");
        System.out.println("\u001B[38;5;216m- adduser \u001B[32m<username> <password> <userId>\u001B[0m[isAdmin:\u001B[38;5;48mtrue\u001B[32m|\u001B[38;5;203mfalse\u001B[0m]: Add a new user.");
        System.out.println("\u001B[38;5;216m- rmuser \u001B[32m<username>\u001B[0m: Remove a user.");
        System.out.println("\u001B[38;5;216m- lsuser\u001B[32m\u001B[0m: List existing users.");
        System.out.println("\u001B[38;5;216m- logout\u001B[32m\u001B[0m: Log out from the current user.");
        System.out.println("\u001B[38;5;216m- stat \u001B[32m<filename>\u001B[0m: Display file information.");
        System.out.println("\u001B[38;5;216m- rm \u001B[32m<filename>\u001B[0m: Remove a file.");
        System.out.println("\u001B[38;5;216m- chown \u001B[32m<newOwnerName> <fileDirectoryName>\u001B[0m: Change file owner.");
        System.out.println("\u001B[38;5;216m- cd \u001B[32m<directoryname>\u001B[0m: Change directory.");
        System.out.println("\u001B[38;5;216m- sudo su\u001B[32m\u001B[0m: Change to root user.");
        System.out.println("\u001B[38;5;216m- exit\u001B[32m\u001B[0m: Exit root user or exit the system.");
        System.out.println("\u001B[38;5;216m- help\u001B[32m\u001B[0m: Display available commands.\n");
    }



}


