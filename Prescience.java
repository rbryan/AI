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
		LibrePD
		ShipStateEnum
		ShipState)
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
			Base
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
	Map<UUID, ShipState> shipStates;

	Random random;
	Set<SpacewarGraphics> workingGraphics = new HashSet<SpacewarGraphics>();
	Map<UUID, AbstractAction> workingActions = new HashMap<UUID, AbstractAction>();
	Map<UUID, ShipState> workingShipStates = new HashMap<UUID, ShipState>();
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
		this.shipStates = new HashMap<UUID, ShipState>();
		workingGraphics = new HashSet<SpacewarGraphics>();
		workingActions = new HashMap<UUID, AbstractAction>();
		workingShipStates = new HashMap<UUID, ShipState>();
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
		Toroidal2DPhysics space = null;

		while(!exit){
			//If we don't have new information to use then wait until
			//we do.
			if(!newKnowledge){
				Thread.yield();
			}else{
				//The Knowledge isn't new anymore
				newKnowledge = false;
				workingActions.clear();
				long ticksSinceKnowledgeUpdate = knowledgeUpdates % 20;
				if(ticksSinceKnowledgeUpdate == 0){
					workingGraphics.clear();

					space = knowledge.getSpace();

					spaceSim = runSimulation(space);
					simulationKnowledge = new Knowledge(spaceSim,knowledge.getTeamObjects());
					
					for(AbstractObject obj : spaceSim.getAllObjects()){
						if(obj instanceof Ship){
							Ship ship = (Ship) obj;
							SpacewarGraphics graphic = new CircleGraphics(1, ship.getTeamColor(), ship.getPosition().deepCopy());
							workingGraphics.add(graphic);

							
						}else{
							SpacewarGraphics graphic = new CircleGraphics(1, Color.RED, obj.getPosition().deepCopy());
							workingGraphics.add(graphic);
						}
					}
				}

				for(AbstractActionableObject obj: knowledge.getTeamObjects()){
					if(obj instanceof Ship){
						Ship ship = (Ship) obj;
						AbstractAction movement = getShipMovement(ship,simulationKnowledge);
						workingActions.put(ship.getId(),movement);

					}
				}
			}

			synchronized(shipStates){
				shipStates.clear();
				shipStates.putAll(workingShipStates);
			}
			synchronized(teamActions){
				teamActions.clear();
				teamActions.putAll(workingActions);
			}

			synchronized(graphics){
				graphics.addAll(workingGraphics);
			}

		}

	}

	public AbstractAction getShipMovement(Ship ship, Knowledge simulationKnowledge){
		if(simulationKnowledge == null)
			simulationKnowledge = knowledge;
	
		Toroidal2DPhysics space = knowledge.getSpace();
		AbstractAction movement = null;
		ShipState state = null;
		ShipStateEnum currentShipState = null;
		Position aimPoint = null;
		Position goal = null;
		AbstractObject goalObject = null;
		 /* To be critically damped, the parameters must satisfy:
		 * 2 * sqrt(Kp) = Kv*/
		LibrePD pdController = new LibrePD(4.47,5,0.8,0.16);


		state = workingShipStates.get(ship.getId());
		
		if(state == null){
			state = new ShipState(ship,aimPoint);
		}else{
			state.setShip(ship);
		}

		currentShipState = state.getState();

		switch(currentShipState){
			case GATHERING_ENERGY:
				goalObject = knowledge.getEnergySources(500).getClosestTo(ship.getPosition());
				goal = goalObject.getPosition();
				state.setShooting(false);
				break;
			case DELIVERING_RESOURCES:
				goalObject = knowledge.getAllTeamObjects()
							.getBases()
							.getClosestTo(ship.getPosition());
				goal = goalObject.getPosition();
				state.setShooting(true);
				break;
			case GATHERING_RESOURCES:
			default:
				goalObject = knowledge.getMineableAsteroids().getClosestTo(ship.getPosition());
				goal = goalObject.getPosition();
				state.setShooting(true);

		}


		aimPoint = simulationKnowledge.getNonActionable().
								getShips().
								getClosestTo(ship.getPosition()).
								getPosition();
		state.setAimPoint(aimPoint);


		workingShipStates.put(ship.getId(),state);

		SpacewarGraphics aimpointgraphic = new CircleGraphics(2, Color.GREEN,aimPoint);
		workingGraphics.add(aimpointgraphic);

		movement = pdController.getRawAction(space,ship.getPosition(),goal,aimPoint);

		return movement;
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
		HashMap<UUID, PurchaseTypes> purchases = new HashMap<UUID, PurchaseTypes>();
		if(purchaseCosts.canAfford(PurchaseTypes.BASE, resourcesAvailable)){
			for(AbstractActionableObject obj : actionableObjects){
				if(obj instanceof Ship){
					boolean safeToMakeBase = true;;
					for(AbstractActionableObject baseMaybe : actionableObjects){
						if(baseMaybe instanceof Base){
							if(space.findShortestDistance(obj.getPosition(),baseMaybe.getPosition()) < 200)
								safeToMakeBase = false;

						}

					}
					if(safeToMakeBase){
						purchases.put(obj.getId(),PurchaseTypes.BASE);
						break;
					}

				}
			}
		}
		return purchases;

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
				ShipState state = null;
				Position aimPoint = null;
				long lastShotTick = 0;

				synchronized(shipStates){
					state = shipStates.get(ship.getId());
					if(state != null){
						aimPoint = state.getAimPoint();
						lastShotTick = state.getLastShotTick();
					}
				}

				if(aimPoint != null && state.getShooting() && knowledgeUpdates - lastShotTick > 1){

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
						synchronized(shipStates){
							state.setLastShotTick(knowledgeUpdates);
						}
						AbstractWeapon newBullet = ship.getNewWeapon(SpaceSettlersPowerupEnum.FIRE_MISSILE);
						if(newBullet != null){
							powerupMap.put(ship.getId(), SpaceSettlersPowerupEnum.FIRE_MISSILE);
						}
					}else{
						powerupMap.put(ship.getId(),SpaceSettlersPowerupEnum.DOUBLE_BASE_HEALING_SPEED);

					}
				}
			}
		}

		return powerupMap;

	}
}
