~(load "macros.scm")

package brya3525;

~(java-import '(
	(java
		(util
			HashMap
			HashSet
			Map
			Random
			Set
			UUID
			(concurrent
			 	ExecutorService
				Executors
				Callable
				Future))
		 (awt
			Color))
	(brya3525
		TextGraphics
		Knowledge
		Prescience
		SpaceSimulation)
	(spacesettlers
		(actions
			AbstractAction
			DoNothingAction
			MoveAction
			PurchaseTypes
			PurchaseCosts)
		(graphics
			CircleGraphics
			SpacewarGraphics)
		(objects
			AbstractActionableObject
			AbstractObject
			Ship
			(powerups
				SpaceSettlersPowerupEnum)
			(resources
				ResourcePile)
			(weapons
				AbstractWeapon))
		(simulator
			Toroidal2DPhysics)
		(utilities
			Position))))

class Prescience extends Thread{
	
	//If set to true the thread will quit.
	boolean exit;

	//My Knowledge representation
	Knowledge knowledge;

	//Space simulation object for predicting ship
	//movements.
	SpaceSimulation spaceSim;

	//Factor to multiply the old timestep by in SpaceSimulation
	final double SIMULATION_TIMESTEP_SCALING_FACTOR = 100;

	//Thread pool for my spaceSim;
	ExecutorService executor;

	//Number of times knowledge object has been updated
	//Used for timeing.
	long knowledgeUpdates;

	//These objects are used for communicating with
	//the outside world. They should be carefully synchronized.
	
	Set<AbstractActionableObject> teamObjects;
	Map<UUID, AbstractAction> teamActions;
	boolean actionsSent;

	HashSet<SpacewarGraphics> graphics;

	/////////////////////////////////////////////////////////

	Random random;

	Prescience(Toroidal2DPhysics space){
		this.random = new Random(20);
		this.spaceSim = new SpaceSimulation(space,space.getTimestep()*SIMULATION_TIMESTEP_SCALING_FACTOR);
		this.knowledge = new Knowledge(space);
		this.graphics = new HashSet<SpacewarGraphics>();
		this.teamObjects = new HashSet<AbstractActionableObject>();
		this.exit=false;
		this.executor = Executors.newSingleThreadExecutor();
		this.knowledgeUpdates = 0;
		Thread.currentThread().setPriority(Thread.currentThread().getPriority()+1);
	}

	public void run(){
		Toroidal2DPhysics space = null;
		Set<SpacewarGraphics> currentGraphics = new HashSet<SpacewarGraphics>();
		Knowledge simulationKnowledge = null;


		while(!exit){
			long ticksSinceKnowledgeUpdate = knowledgeUpdates % 100;
			if(ticksSinceKnowledgeUpdate == 0){
				currentGraphics.clear();
				synchronized(knowledge){
					space = knowledge.getSpace();
				}
				spaceSim = new SpaceSimulation(space,space.getTimestep()*SIMULATION_TIMESTEP_SCALING_FACTOR);
				Future<SpaceSimulation> future = executor.submit(spaceSim);
				try{
					spaceSim = future.get();
				}catch(Exception e){
					e.printStackTrace();
					//throw new Exception("spaceSim Future.get() threw and exception!!!",e);
				}
				while(!future.isDone()){
					Thread.yield();
				}
				
				for(AbstractObject obj : spaceSim.getAllObjects()){
					if(obj instanceof Ship){
						Ship ship = (Ship) obj;
						SpacewarGraphics graphic = new CircleGraphics(1, ship.getTeamColor(), ship.getPosition().deepCopy());
						currentGraphics.add(graphic);

						
					}else{
						SpacewarGraphics graphic = new CircleGraphics(1, Color.RED, obj.getPosition().deepCopy());
						currentGraphics.add(graphic);
					}
				}
			}

			for(AbstractActionableObject obj: teamObjects){
				if(obj instanceof Ship){
					Ship ship = (Ship) obj;
					if(simulationKnowledge != null){
						continue;

					}
				}
			}

			synchronized(graphics){
				graphics.addAll(currentGraphics);
			}

		}

	}


	//The following functions are the external interface used by 
	//my team client

	public void exit(){
		exit = true;
		synchronized(executor){
			executor.shutdownNow();
		}
	}

	public Map<UUID, AbstractAction> 
			getMovementStart(Toroidal2DPhysics space,
					 Set<AbstractActionableObject> actionableObjects) {


				//Update what we know about the world
				//This space object is what will be used
				//throughout my AI
				synchronized(knowledge){
					knowledge.update(space);
					knowledgeUpdates++;
				}

				return new HashMap<UUID, AbstractAction>();

	
	}

	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
	}

	public Set<SpacewarGraphics> getGraphics() {
		HashSet<SpacewarGraphics> newGraphics;
		synchronized(graphics){
			newGraphics = new HashSet<SpacewarGraphics>(graphics);
			graphics.clear();
		}
		return newGraphics;
	}


	/**
	 * Random never purchases 
	 */
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects, 
			ResourcePile resourcesAvailable, 
			PurchaseCosts purchaseCosts) {
		return new HashMap<UUID,PurchaseTypes>();

	}

	/**
	 * This is the new way to shoot (and use any other power up once they exist)
	 */
	public Map<UUID, SpaceSettlersPowerupEnum> getPowerups(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, SpaceSettlersPowerupEnum> powerupMap = new HashMap<UUID, SpaceSettlersPowerupEnum>();
		
		return powerupMap;
		
	}

}
