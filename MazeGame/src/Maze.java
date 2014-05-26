import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
/*
 * IDEAS:
 * Enemy player
 * Weapons for player to collect to kill enemy
 * Collecting items for the next level (powerups)
 * Treasure chest to get high scores for a maze
 * Score dependent on time taken to finish maze
 * Have a plot with dialogue boxes coming up for each level with picture displayed
 */

/**
 * Contains the board of the maze.
 * It contains the maze-generating algorithm
 * and the maze solving algorithm.
 * @author Gavin Tam
 * @see Tile
 * @see Player
 */
public class Maze {
	private Tile[][] grid;
	private int width;
	private int height;
	private Player player;	//location of the current player, null if dead
	private Enemy[] enemy;	//location of the enemy, null if dead
	
	private HashMap<Tile,Tile> mazeSolution;
			
	public Maze (int w, int h, Player p) {
		//width and height should be an odd number
		//width and height is assumed to be greater than 1
		grid = new Tile[w+2][h+2];
		this.width = w+2;
		this.height = h+2;
		player = p;	//use input player
		enemy = new Enemy[(int)(((width-13)/3)+1)];	//make new enemy
		for (int i = 0; i < enemy.length; i++) {
			enemy[i] = new Enemy();
		}
		createMaze();	//initialise all tiles
		
		final Timer timer = new Timer();	//auto-scheduling of enemy movement
		timer.schedule(new TimerTask() {
			public void run() {
				for (int i = 0; i < enemy.length; i++) {
					if (player.isDead()) {
						timer.cancel();
					} else if (enemy[i].isDead()) {
						continue;
					} else if (player.isIcePowerCollected()) {
						try {
							Thread.sleep(5000);	//enemy freezes for 5 seconds
							player.setIcePowerCollected(false);
						} catch (InterruptedException e) {
							//do nothing
						}
					} else if (player.getLocation() != null && !player.isDead()) {
						//enemy follows player
						HashMap<Tile,Tile> path = new HashMap<Tile,Tile>();
						path.put(grid[enemy[i].getLocation().getX()][enemy[i].getLocation().getY()], null);
						boolean[][] visitedTile = new boolean[width][height];
						findPath(enemy[i].getLocation().getX(), enemy[i].getLocation().getY(),
								player.getLocation().getX(), player.getLocation().getY(),
								visitedTile,path);
						Tile curr = player.getLocation();
						while (!path.get(curr).equals(enemy[i].getLocation())) {	//if we haven't reached the starting state yet
							curr = path.get(curr);
						}
						enemy[i].setLocation(curr);
						if (enemy[i].getLocation().equals(player.getLocation())) {
							if (!swordCollected()) {
								player.setDead(true);	//player dies and enemy stops moving
							} else {
								enemy[i].setDead(true);
								player.addEnemyKilled();
							}
						}
					} //else don't move enemy
					//showMaze();	//for debugging
				}
			}
		},1500,1500);	//enemy moves every 1 second 
						//if too fast, player cannot reach sword and will eventually die
	}
	
	/**
	 * Generate the maze.
	 * This should be called only once when initialising the maze.
	 */
	private void createMaze() {
		//initialise maze as all non-walkable tiles
		//and assign a random weight to each tile
		final double weights[][] = new double[width][height];
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				Tile t = new Tile(false,i,j); // false denotes non-walkable tile
				grid[i][j] = t;
				weights[i][j] = Math.random();
			}
		}
		
		//Use Prim's algorithm on a randomly weighted grid graph representation of the maze
		//vertices are represented by tiles with odd coordinates
		//and edges are represented by two adjacent vertices (i.e. a series of 3 consecutive tiles)
		PriorityQueue<TileEdge> edges = new PriorityQueue<TileEdge>(100, new Comparator<TileEdge>() { 
			public int compare (TileEdge e1, TileEdge e2) {
				double weightDiff = (weights[e1.getTile0().getX()][e1.getTile0().getY()] +
						weights[e1.getTile1().getX()][e1.getTile1().getY()] +
						weights[e1.getTile2().getX()][e1.getTile2().getY()])
						-(weights[e2.getTile0().getX()][e2.getTile0().getY()]
						+ weights[e2.getTile1().getX()][e2.getTile1().getY()]
						+ weights[e2.getTile2().getX()][e2.getTile2().getY()]);
				if (weightDiff > 0) return 1;
				if (weightDiff < 0) return -1;
				return 0;
			}
		});
		HashSet<TileEdge> edgesAdded = new HashSet<TileEdge>();
		HashSet<Tile> visited = new HashSet<Tile>();
		ArrayList<TileEdge> neighbours = getNeighbouringEdges(grid[1][1]);
		for (int i = 0; i < neighbours.size(); i++) {
			edges.add(neighbours.get(i));
			edgesAdded.add(neighbours.get(i));
		}
		visited.add(grid[1][1]);
		int numVertices = ((width-1)/2)*((height-1)/2);
		//while not all vertices have been visited
		while (visited.size() < numVertices) {
			TileEdge curr = edges.remove();
			if ((visited.contains(curr.getTile0()) && visited.contains(curr.getTile2()))
				|| (!visited.contains(curr.getTile0()) && !visited.contains(curr.getTile2()))) {
				continue;
			}
			//only one of the tiles of the edge is unvisited
			if (visited.contains(curr.getTile0())) {
				neighbours = getNeighbouringEdges(curr.getTile2());
				visited.add(curr.getTile2());	//if Tile0 has been visited, Tile2 must not have been visited
			} else {
				neighbours = getNeighbouringEdges(curr.getTile0());
				visited.add(curr.getTile0());	//if Tile2 has been visited, Tile0 must not have been visited
			}
			for (int i = 0; i < neighbours.size(); i++) {
				if (!edgesAdded.contains(neighbours.get(i))) {
					edges.add(neighbours.get(i));
					edgesAdded.add(neighbours.get(i));
				}
			}
			curr.getTile0().setWalkable();		//set the edge as a walkable path
			curr.getTile1().setWalkable();
			curr.getTile2().setWalkable();
		}
		//set player and enemy locations
		player.setLocation(grid[1][1]);	//origin at (1,1), 
		int numEnemy = 0;
		while (numEnemy < enemy.length) {	//enemy spawns on bottom half of the screen
			int randomX = 1 + (int)(Math.random()*(width-2));
			int randomY = (height-2)/2 + (int)(Math.random()*((height-2)/2));
			if (grid[randomX][randomY].getType() == Tile.PATH &&	//check that the tile is walkable
				!grid[randomX][randomY].equals(player.getLocation())) {
				enemy[numEnemy].setLocation(grid[randomX][randomY]);	//enemy starts randomly
				numEnemy++;
			}
		}
		
		//set special tiles
		grid[1][0].setType(Tile.DOOR);	//set tile just above the origin to a door
		grid[width-2][height-1].setType(Tile.DOOR);	//set tile just below the destination to a door

		grid[1][height-2].setType(Tile.KEY);	//set bottom left corner to key to door for now
		grid[width-2][1].setType(Tile.SWORD);
		
		int numIcePower = 0;
		//first ice power is always in the top left quadrant
		while (true) {
			int randomX = 1 + (int)(Math.random()*((width-2)/2));
			int randomY = 1 + (int)(Math.random()*((height-2)/2));
			if (grid[randomX][randomY].getType() == Tile.PATH &&	//check that the tile is walkable
				!grid[randomX][randomY].equals(player.getLocation())) {
				grid[randomX][randomY].setType(Tile.ICE_POWER);
				numIcePower++;
				break;
			}
		}
		//other ice powers can spawn elsewhere
		while (numIcePower < enemy.length-1) {	//one ice power for each enemy, minus one (harder)
			int randomX = 1 + (int)(Math.random()*(width-2));
			int randomY = 1 + (int)(Math.random()*(height-2));
			if (grid[randomX][randomY].getType() == Tile.PATH &&	//check that the tile is walkable
				!grid[randomX][randomY].equals(player.getLocation())) {
				grid[randomX][randomY].setType(Tile.ICE_POWER);
				numIcePower++;
			}
		}
		
		//sets three random tiles to treasure
		int i = 0;
		while (i < (int)(width-13)/3 + 3) {
			int randomX = 1 + (int)(Math.random()*((width-2)));
			int randomY = 1 + (int)(Math.random()*((height-2)));
			if (grid[randomX][randomY].getType() == Tile.PATH &&	//check that the tile is walkable
				!grid[randomX][randomY].equals(player.getLocation())) {		//and not the origin
				grid[randomX][randomY].setType(Tile.TREASURE);
				i++;
			}
		}
		//width and height should be big enough to allow this to be valid
		mazeSolution = new HashMap<Tile,Tile>();
		mazeSolution.put(grid[1][1], null);
		boolean[][] visitedTile = new boolean[width+2][height+2];
		findPath(1,1,visitedTile,mazeSolution);		//finds solution to maze
		//if (solvable) {	//maze should always be solvable
		showMaze();			//for debugging
		//}
	}
	
	public void showMaze () {
		Tile playerLoc = player.getLocation();
		//Tile enemyLoc = enemy.getLocation();
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				if (grid[i][j].isWalkable()) {
					//order of printing is important as path contains start and dest
					if (playerLoc != null && playerLoc.equals(grid[i][j])) {
						System.out.print("P");
					} else if (i == (width-1)-1 && j == (height-1)-1) {
						System.out.print("D");
					} /*else if (enemyLoc.equals(grid[i][j])) {
						System.out.print("E");
					}*/ else {
						List<Tile> path = giveHint(grid[1][height-2]);
						if (path.contains(grid[i][j])) {
							System.out.print("X");
						} else if (mazeSolution.containsKey(grid[i][j])) {
							System.out.print("*");
						} else {
							System.out.print("0");
						}
					}
				} else {
					System.out.print("-");	//represents wall
				}
			}
			System.out.println();
		}
	}
	
	//checking within a 2 tile radius
	public ArrayList<TileEdge> getNeighbouringEdges (Tile curr) {
		ArrayList<TileEdge> neighbouringEdges = new ArrayList<TileEdge>();
		//vertices exist only on tiles with odd x and y coordinates
		for (int i = Math.max(curr.getX()-2,1); i <= Math.min(curr.getX()+2,width-1); i+=2) {
			for (int j = Math.max(curr.getY()-2,1); j <= Math.min(curr.getY()+2,height-1); j+=2) {
				TileEdge newEdge = null;
				if (curr.getX()-i == 2 && curr.getY()-j == 0) {
					newEdge = new TileEdge(grid[i][curr.getY()],
										   grid[i+1][curr.getY()],curr);
				} else if (curr.getX()-i == -2 && curr.getY()-j == 0) {
					newEdge = new TileEdge(curr,grid[i-1][curr.getY()],
												grid[i][curr.getY()]);
				} else if (curr.getX()-i == 0 && curr.getY()-j == 2) {
					newEdge = new TileEdge(grid[curr.getX()][j],
										   grid[curr.getX()][j+1], curr);
				} else if (curr.getX()-i == 0 && curr.getY()-j == -2) {
					newEdge = new TileEdge(curr, grid[curr.getX()][j-1],
												 grid[curr.getX()][j]);
				} else {
					continue;
				}
				neighbouringEdges.add(newEdge);
			}
		}
		return neighbouringEdges;
	}
	
	/**
	 * Finds the path from (x,y) to the destination.
	 * It stores the path in an input hash map,
	 * which maps the tile to its parent tile.
	 * Credit to http://en.wikipedia.org/wiki/Maze_solving_algorithm.
	 * Adapted from "Recursive algorithm".
	 * @return true if a path exists. It should be true
	 * as the maze is always solvable from any position.
	 */
	public boolean findPath (int x, int y, boolean[][] visited, HashMap<Tile,Tile> parent) {
		if (x == width-2 && y == height-2) return true;
		if (!grid[x][y].isWalkable() || visited[x][y]) return false;
		visited[x][y] = true;
		if (x != 0) {
			if (findPath(x-1,y,visited,parent)) {
				parent.put(grid[x-1][y], grid[x][y]);
				return true;
			}
		}
		if (x != width-1) {
			if (findPath(x+1,y,visited,parent)) {
				parent.put(grid[x+1][y], grid[x][y]);
				return true;
			}
		}
		if (y != 0) {
			if (findPath(x,y-1,visited,parent)) {
				parent.put(grid[x][y-1], grid[x][y]);
				return true;
			}
		}
		if (y != height-1) {
			if (findPath(x,y+1,visited,parent)) {
				parent.put(grid[x][y+1], grid[x][y]);
				return true;
			}
		}
		return false;
	}
	
	public boolean findPath (int x, int y, int x1, int y1, boolean[][] visited, HashMap<Tile,Tile> parent) {
		if (x == x1 && y == y1) return true;
		if (!grid[x][y].isWalkable() || visited[x][y]) return false;
		visited[x][y] = true;
		if (x != 0) {
			if (findPath(x-1,y,x1,y1,visited,parent)) {
				parent.put(grid[x-1][y], grid[x][y]);
				return true;
			}
		}
		if (x != width-1) {
			if (findPath(x+1,y,x1,y1,visited,parent)) {
				parent.put(grid[x+1][y], grid[x][y]);
				return true;
			}
		}
		if (y != 0) {
			if (findPath(x,y-1,x1,y1,visited,parent)) {
				parent.put(grid[x][y-1], grid[x][y]);
				return true;
			}
		}
		if (y != height-1) {
			if (findPath(x,y+1,x1,y1,visited,parent)) {
				parent.put(grid[x][y+1], grid[x][y]);
				return true;
			}
		}
		return false;
	}
	
	//give first 10 steps from current tile to destination
	public List<Tile> giveHint(Tile t) {
		//TODO change colour of tiles such that path from current player position
		//to the nearest tile part of the solution is given
		boolean[][] visited = new boolean[width][height];
		HashMap<Tile,Tile> parent = new HashMap<Tile,Tile>();
		parent.put(t, null);	//current tile has no parent as it is the starting tile of the path
		boolean pathFound = findPath(t.getX(), t.getY(), visited, parent);
		if (!pathFound) {
			return null;		//error in pathfinding
		} else {
			LinkedList<Tile> path = new LinkedList<Tile>();
			Tile curr = grid[width-2][height-2];	//backtrack from destination
			path.addFirst(curr);
			while (parent.get(curr) != null) {	//if we haven't reached the starting state yet
				Tile prev = parent.get(curr);
				path.addFirst(prev);
				curr = prev;
			}
			List<Tile> pathInit = path.subList(0,10);
			return pathInit;
		}
	}
	
	/**
	 * Finds the tile on the maze that the player is situated in.
	 * @return the tile in which the player is on.
	 */
	public Tile getPlayerTile() {
		if (player.isDead()) {
			return null;
		}
		return player.getLocation();
	}
	
	/**
	 * Finds the tile on the maze that the enemy is situated in.
	 * @return the tile in which the enemy is on.
	 */
	public Tile getEnemyTile(int i) {
		if (i >= enemy.length || enemy[i].isDead()) {
			return null;
		}
		return enemy[i].getLocation();
	}
	
	public Tile getDestDoor() {
		return grid[width-2][height-1];
	}
	
	/**
	 * Updates the player location if a valid move is taken.
	 * See isValid() for conditions.
	 * @param x the amount of movement in the x direction, as per number of tiles.
	 * The number is negative if the movement is to the left direction.
	 * @param y the amount of movement in the y direction, as per number of tiles.
	 * The number is negative if the movement is to the up direction.
	 */
	public void updatePlayerLoc (int x, int y) {
		if (isValid(x,y) && !player.isDead()) {
			player.setLocation(grid[player.getLocation().getX()+x][player.getLocation().getY()+y]);
			Tile playerLoc = player.getLocation();	//get updated player location
			for (int i = 0; i < enemy.length; i++) {
				Tile enemyLoc = enemy[i].getLocation();	//get enemy location
				if (playerLoc.equals(enemyLoc) && !enemy[i].isDead()) {
					if (!swordCollected() && !icePowerCollected()) {
						player.setDead(true);	//player dies and enemy stops moving
					} else {
						enemy[i].setDead(true);
						player.addEnemyKilled();
					}
				}
			}
			if (playerLoc.getType() == Tile.KEY) {
				player.setKeyCollected(true);
				playerLoc.setType(Tile.PATH);	//set key tile to normal path
			} else if (playerLoc.getType() == Tile.TREASURE) {
				player.addNumTreasureCollected();
				playerLoc.setType(Tile.PATH);	//if we collected the treasure
			} else if (playerLoc.getType() == Tile.SWORD) {
				player.setSwordCollected(true);
				playerLoc.setType(Tile.PATH);
			} else if (playerLoc.getType() == Tile.ICE_POWER) {
				player.setIcePowerCollected(true);
				playerLoc.setType(Tile.PATH);
			}
			checkReachedEnd();	//unlock door if player has reached end tile
		}
	}
	
	/**
	 * Checks if a player is dead.
	 * A player is dead if it encounters the enemy.
	 * @return true if the player is dead.
	 */
	public boolean playerDied () { return player.isDead(); }
	public boolean enemyDied (int i) { return enemy[i].isDead(); }
	public boolean allEnemyDied () { 
		for (int i = 0; i < enemy.length; i++) {
			if (!enemy[i].isDead()) {
				return false;
			}
		}
		return true; 
	}
	public int getNumEnemies () {
		return enemy.length;
	}
	
	/**
	 * Checks if a player move is valid.
	 * The player can only move one tile from its original position
	 * in either direction.
	 * It cannot move over the border of the maze,
	 * or walk onto a wall.
	 * @param x the amount of movement in the x direction, as per number of tiles.
	 * The number is negative if the movement is to the left direction.
	 * @param y the amount of movement in the y direction, as per number of tiles.
	 * The number is negative if the movement is to the up direction.
	 * @return true if the player move is valid.
	 */
	public boolean isValid (int x, int y) {
		Tile playerLoc = player.getLocation();
		if (x > 1 || x < -1 || y > 1 || y < -1) {	//can only move at most one tile per turn
			return false;
		} else if (x != 0 && y != 0) {	//can only move in one direction
			return false;
		} else if ((playerLoc.getX()+x) > (width-1) || (playerLoc.getY()+y) > (height-1)
			|| (playerLoc.getX()+x) < 0 || (playerLoc.getY()+y) < 0) {	//cannot move over border
			return false;
		} else if (!grid[playerLoc.getX()+x][playerLoc.getY()+y].isWalkable()) { //cannot move to wall
			return false;
		}
		return true;
	}
	
	/**
	 * Checks if the player has reached the destination.
	 * The destination is the tile right before the exit door.
	 * If the key is collected, then the door is unlocked as well;
	 * otherwise, it remains closed.
	 * To check if the player takes the exit, see exitedMaze().
	 * @return true if the player has reached the destination.
	 */
	public boolean checkReachedEnd() {
		Tile playerLoc = player.getLocation();
		if (player.isDead()) {
			return false;
		}
		//destination is at (width-2, height-2)
		boolean atEnd = false;
		if (playerLoc.getX() == (width-2) && playerLoc.getY() == (height-2)) {
			atEnd = true;
			if (keyCollected()) {	//if key is collected, unlock door
				grid[width-2][height-1].setType(Tile.PATH);	//set door to walkable path
				grid[width-2][height-1].setWalkable();
			}
		}
		return atEnd;
	}
	
	/**
	 * Checks if the player is at the origin.
	 * The origin of the maze is at (1,1),
	 * just in front of the entrance door.
	 * @return true if the player is at the origin.
	 */
	public boolean atStart() {
		return (!player.isDead() && player.getLocation().getX() == 1 && player.getLocation().getY() == 1);
	}
	
	/**
	 * Checks if the player has completed the maze.
	 * It is completed when the destination door is opened by the key
	 * and the player is located on the exit, where the door was originally
	 * positioned.
	 * @return true if player has completed the maze.
	 */
	//see if player unlocked door and took the exit
	public boolean exitedMaze() {
		return (!player.isDead() && player.getLocation().getX() == (width-2) && player.getLocation().getY() == (height-1));
	}
	
	public int getNumTreasureCollected() { return player.getNumTreasureCollected(); }
	public boolean keyCollected() { return player.isKeyCollected(); }
	public boolean swordCollected() { return player.isSwordCollected(); }
	public boolean icePowerCollected() { return player.isIcePowerCollected(); }
	
	/**
	 * Get the tile of a specific set of x and y coordinates.
	 * (0,0) is the top left corner of the maze (includes border).
	 * If a set of coordinates outside of the bounds of the maze is entered,
	 * the null tile is returned.
	 * @param x the x coordinate of the tile.
	 * @param y the y coordinate of the tile.
	 * @return the tile with the specified x and y coordinates, or null if
	 * the coordinates are outside of the maze area.
	 */
	public Tile getTile(int x, int y) {
		if (x >= 0 && y >= 0 && x <= (width-1) && y <= (height-1)) {
			return this.grid[x][y];
		} else {
			return null;
		}
	}
	/**
	 * Get the width of the maze (including border).
	 * @return the width of the maze, as per the number of tiles.
	 */
	public int getWidth() { return this.width; }
	/**
	 * Get the length of the maze (including border).
	 * @return the length of the maze, as per the number of tiles.
	 */
	public int getHeight() { return this.height; }
	/**
	 * Gets the grid of tiles of the maze.
	 * @return the grid of tiles of the maze.
	 */
	public Tile[][] getGrid() { return this.grid; }
}