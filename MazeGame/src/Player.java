
public class Player {
	private String name;
	private String character;
	private Tile location;
	private boolean isDead;
	
	private boolean keyCollected;
	private boolean swordCollected;
	private int numTreasureCollected;
	private int enemyKilled;

	public Player(String name, String character){
		this.name = name;
		this.character = character;
	}
	
	public Player() {
		//nothing, used by maze when character and name is not important
	}
	
	//Return the name of the player
	public String getName(){
		return this.name;
	}
	
	//Return the character this player has selected
	public String getCharacter(){
		return this.character;
	}
	
	//Return the character this player has selected
	public void setCharacter(String character){
		this.character = character;
	}
	
	public Tile getLocation () {
		return location;
	}
	
	public void setLocation (Tile t) {
		location = t;
	}
	
	public void setDead (boolean dead) {
		isDead = dead;
	}

	public boolean isDead() {
		return isDead;
	}

	public boolean isKeyCollected() {
		return keyCollected;
	}

	public void setKeyCollected(boolean keyCollected) {
		this.keyCollected = keyCollected;
	}

	public boolean isSwordCollected() {
		return swordCollected;
	}

	public void setSwordCollected(boolean swordCollected) {
		this.swordCollected = swordCollected;
	}

	public int getNumTreasureCollected() {
		return numTreasureCollected;
	}

	public void addNumTreasureCollected() {
		this.numTreasureCollected++;
	}
	
	public int getEnemyKilled() {
		return enemyKilled;
	}

	public void addEnemyKilled() {
		this.enemyKilled++;
	}
	
	public void clearStats() {
		location = null;
		isDead = false;	
		keyCollected = false;
		swordCollected = false;
		numTreasureCollected = 0;
		enemyKilled = 0;
	}
}
