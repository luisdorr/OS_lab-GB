import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Inode {
     String ownerId;
     LocalDateTime createdDate;
    LocalDateTime lastAccessedDate;
    LocalDateTime lastModifiedDate;
     Map<String, Map<String, Boolean>> permissions;
     boolean isDirectory;
     int size;
     List<Block> blocks;
     int blockCount;
    Block singleIndirectBlock;

    public Inode(String ownerId, boolean isDirectory) {
        this.ownerId = ownerId;
        this.createdDate = LocalDateTime.now();
        this.lastAccessedDate = this.createdDate;
        this.lastModifiedDate = this.createdDate;
        this.permissions = new HashMap<>();
        this.permissions.put("user", new HashMap<>());
        this.permissions.put("group", new HashMap<>());
        this.permissions.put("other", new HashMap<>());
        this.permissions.get("user").put("read", true);
        this.permissions.get("user").put("write", true);
        this.permissions.get("user").put("execute", false);
        this.permissions.get("group").put("read", true);
        this.permissions.get("group").put("write", false);
        this.permissions.get("group").put("execute", false);
        this.permissions.get("other").put("read", true);
        this.permissions.get("other").put("write", false);
        this.permissions.get("other").put("execute", false);
        this.isDirectory = isDirectory;
        this.size = 0;
        this.blocks = new ArrayList<>();
        this.blockCount = 0;
        this.singleIndirectBlock = null;
    }

    public String inodeInfo() {
        if (this.isDirectory) {
            return "  Size: " + this.size + "               Blocks: " + this.blockCount + "               directory\nAccess: (" + this.buildPermissionStringFormat() + ")    Uid: " + this.ownerId + "\nAccess: " + this.lastAccessedDate + "\nModify: " + this.lastModifiedDate + "\n Birth: " + this.createdDate;
        } else {
            return "  Size: " + this.size + "               Blocks: " + this.blockCount + "               file\nAccess: (" + this.buildPermissionStringFormat() + ")    Uid: " + this.ownerId + "\nAccess: " + this.lastAccessedDate + "\nModify: " + this.lastModifiedDate + "\n Birth: " + this.createdDate;
        }
    }

    public String buildPermissionStringFormat() {
        String permissionString = "";
        if (this.isDirectory){
            permissionString += "d";
        } else {
            permissionString += "-";
        }
        for (Map.Entry<String, Map<String, Boolean>> entry : this.permissions.entrySet()) {
            Map<String, Boolean> permission = entry.getValue();
            permissionString += permission.get("read") ? "r" : "-";
            permissionString += permission.get("write") ? "w" : "-";
            permissionString += permission.get("execute") ? "x" : "-";
        }
        return permissionString;
    }
}