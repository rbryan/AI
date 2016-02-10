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
		SpaceSimulation
		LibrePD)
	(spacesettlers
		(actions
			AbstractAction
			DoNothingAction
			MoveAction
			RawAction
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
			Position
			Vector2D))))

class Prescience extends Thread{
	
	//If set to true the thread will quit.
	boolean exit;

	//My Knowledge representation
	Knowledge knowledge;
	//has to be a Boolean class for synchronized()
	boolean newKnowledge;

	//Space simulation object for predicting ship
	//movements.
	SpaceSimulation spaceSim;
	Knowledge simulationKnowledge;

	//Factor to multiply the old timestep by in SpaceSimulation
	final double SIMULATION_TIMESTEP_SCALING_FACTOR = 20;

	//Thread pool for my spaceSim;
	ExecutorService executor;

	//Number of times knowledge object has been updated
	//Used for timeing.
	long knowledgeUpdates;

	//These objects are used for communicating with
	//the outside world. They should be carefully synchronized.
	
	Map<UUID, AbstractAction> teamActions;

	HashSet<SpacewarGraphics> graphics;
	Map<UUID, Position> aimPoints;

	Random random;
	/////////////////////////////////////////////////////////


	Prescience(Toroidal2DPhysics space){
		this.spaceSim = new SpaceSimulation(space,space.getTimestep()*SIMULATION_TIMESTEP_SCALING_FACTOR);
		this.knowledge = new Knowledge(space);
		this.newKnowledge = false;
		this.graphics = new HashSet<SpacewarGraphics>();
		this.teamActions = new HashMap<UUID, AbstractAction>();
		this.exit=false;
		this.executor = Executors.newSingleThreadExecutor();
		this.knowledgeUpdates = 0;
		this.aimPoints = new HashMap<UUID, Position>();
		random = new Random(20);
		//Look at me being such a nice person and not causing everyone else
		//to time out by setting my thread priority too high.
		Thread.currentThread().setPriority(Thread.currentThread().getPriority()+1);
	}

	public SpaceSimulation runSimulation(Toroidal2DPhysics space){
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

					return spaceSim;


	}

	public void run(){
		try{
		Toroidal2DPhysics space = null;
		Set<SpacewarGraphics> currentGraphics = new HashSet<SpacewarGraphics>();
		Map<UUID, AbstractAction> currentActions = new HashMap<UUID, AbstractAction>();
		AbstractObject base = null;


		while(!exit){
			//If we don't have new information to use then wait until
			//we do.
			if(!newKnowledge){
				Thread.yield();
			}else{
				//The Knowledge isn't new anymore
				newKnowledge = false;
				currentActions.clear();
				long ticksSinceKnowledgeUpdate = knowledgeUpdates % 20;
				if(ticksSinceKnowledgeUpdate == 0){
					currentGraphics.clear();

					synchronized(knowledge){
						space = knowledge.getSpace();
					}

					spaceSim = runSimulation(space);
					simulationKnowledge = new Knowledge(spaceSim,knowledge.getTeamObjects());
					
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

				for(AbstractActionableObject obj: knowledge.getTeamObjects()){
					synchronized(aimPoints){
						aimPoints.clear();
					}
					if(obj instanceof Ship){
						Ship ship = (Ship) obj;
						if(simulationKnowledge != null){
							Position aimPoint = null;
							Position goal = null;
							AbstractObject goalObject = null;

							 /* To be critically damped, the parameters must satisfy:
							 * 2 * sqrt(Kp) = Kv*/
							LibrePD pdController = new LibrePD(4.47,5,0.8,0.16);

							if(ship.getEnergy() < 2000 && base != null){
								goalObject = knowledge.getEnergySources().getClosestTo(ship.getPosition());
								goal = goalObject.getPosition();
							}else if(ship.getMass() > 300){
								goalObject = knowledge.getAllTeamObjects()
											.getBases()
											.getClosestTo(ship.getPosition());
								goal = goalObject.getPosition();
							}else{
								goalObject = knowledge.getMineableAsteroids().getClosestTo(ship.getPosition());
								goal = goalObject.getPosition();
							}


							aimPoint = simulationKnowledge.getNonActionable().
													getShips().
													getClosestTo(ship.getPosition()).
													getPosition();

							AbstractAction movement = pdController.getRawAction(space,ship.getPosition(),goal,aimPoint);

							currentActions.put(ship.getId(),movement);

							SpacewarGraphics aimpointgraphic = new CircleGraphics(5, Color.GREEN,aimPoint);
							currentGraphics.add(aimpointgraphic);
								


							synchronized(aimPoints){
								aimPoints.put(ship.getId(),aimPoint);
							}


						}
					}else{
						base = obj;
					}
				}
			}

			synchronized(teamActions){
				teamActions.clear();
				teamActions.putAll(currentActions);
			}

			synchronized(graphics){
				graphics.addAll(currentGraphics);
			}

		}}catch(Exception e){
			e.printStackTrace();
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
				Map<UUID,AbstractAction> newActions;

				//Update what we know about the world
				//This space object is what will be used
				//throughout my AI
				synchronized(knowledge){
					knowledge.update(space,actionableObjects);
					knowledgeUpdates++;
				}
				newKnowledge = true;

				synchronized(teamActions){
					newActions = new HashMap<UUID, AbstractAction>(teamActions);
				}
				return newActions;
	
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

		for(AbstractActionableObject obj : actionableObjects){
			if(obj instanceof Ship){
				Ship ship = (Ship) obj;
				Position aimPoint = null;

				synchronized(aimPoints){
					aimPoint = aimPoints.get(ship.getId());
				}
				if(aimPoint != null){

					Vector2D aimVector = space.findShortestDistanceVector(ship.getPosition(),aimPoint);
					double aimDistance = aimVector.getMagnitude();

					Vector2D shipPosition = new Vector2D(ship.getPosition().getX(),ship.getPosition().getY());
					Vector2D aimPointPosition = new Vector2D(aimPoint.getX(),aimPoint.getY());

					double shootProbability =  Math.pow((
									1/Math.abs((shipPosition.getAngle() - 
										aimVector.getAngle()))
											* aimDistance /20 *
									1/Math.abs(aimDistance/50 - knowledgeUpdates %
										SIMULATION_TIMESTEP_SCALING_FACTOR)/5),2);

					if(aimDistance < 200 && random.nextDouble() < shootProbability ){
						//shoot
						AbstractWeapon newBullet = ship.getNewWeapon(SpaceSettlersPowerupEnum.FIRE_MISSILE);
						if(newBullet != null){
							powerupMap.put(ship.getId(), SpaceSettlersPowerupEnum.FIRE_MISSILE);
						}
/*
						SpacewarGraphics shot = new CircleGraphics(1,Color.RED,ship.getPosition());
						synchronized(graphics){
							graphics.add(shot);
						}
*/


					}
				}
			}

		}
		return powerupMap;
		
	}

}
