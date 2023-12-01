import java.util.ArrayList;
import java.util.List;

class Block {
    private List<Byte> content;
    private int bloockSize;
    
    public Block(int blockSize) {
        this.content = new ArrayList<>();
        this.bloockSize = blockSize;
    }
    
    public void write(List<Byte> newContent) {
        this.content = newContent;
    }
    
    public List<Byte> read() {
        return this.content;
    }
}
