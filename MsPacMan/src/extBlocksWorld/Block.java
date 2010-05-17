package extBlocksWorld;

/**
 * A block in blocks world.
 * 
 * @author Sam Sarjant
 */
public class Block {
	private String name_;
	
	public Block (String name) {
		name_ = name;
	}
	
	public String getName() {
		return name_;
	}
	
	@Override
	public boolean equals(Object obj) {
		if ((obj != null) && (obj instanceof Block)) {
			Block block = (Block) obj;
			if (name_.equals(block.name_))
				return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return name_;
	}
}
