
public class Enemy {
	boolean isDead;
	Tile location;
	
	public Enemy (Tile t) {
		location = t;
	}
	
	public Enemy () {
		//nothing, used by maze with no location is decided yet
	}
	
	public void setDead () {
		isDead = true;
		location = null;
	}

	public boolean isDead() {
		return isDead;
	}
	
	public Tile getLocation () {
		return location;
	}
	
	public void setLocation (Tile t) {
		location = t;
	}
}