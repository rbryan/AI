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
		ShipState
		ShipStateEnum)
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

class ShipState{
	Ship ship;
	Position aimPoint;
	ShipStateEnum state;
	boolean shooting;
	long lastShotTick;
	

	public ShipState(Ship ship,Position aimPoint){

		this.ship = ship;
		this.aimPoint = aimPoint;
		state = ShipStateEnum.GATHERING_RESOURCES;
		long lastShotTick = 0;
		boolean shooting = true;
		stateUpdate();

	}

	public boolean getShooting(){
		return shooting;
	}

	public void setShooting(boolean val){
		shooting = val;
	}

	public ShipStateEnum getState(){
		stateUpdate();
		return state;
	}

	public Position getAimPoint(){
		return aimPoint;
	}

	public void setAimPoint(Position newAimPoint){
		aimPoint = newAimPoint;
	}

	public Ship getShip(){
		return ship;
	}

	public void setShip(Ship newShip){
		ship = newShip;
	}

	public long getLastShotTick(){
		return lastShotTick;
	}

	public void setLastShotTick(long newLastShotTick){
		lastShotTick = newLastShotTick;
	}

	public void stateUpdate(){
		switch(state){
			case GATHERING_RESOURCES:
				if(ship.getEnergy() < 2500){
					state = ShipStateEnum.GATHERING_ENERGY;
				}else if(ship.getMass() > 300){
					state = ShipStateEnum.DELIVERING_RESOURCES;
				}
				break;
			case GATHERING_ENERGY:
				if(ship.getEnergy() > 3500){
					if(ship.getMass() > 300){
						state = ShipStateEnum.DELIVERING_RESOURCES;
					}else{
						state = ShipStateEnum.GATHERING_RESOURCES;
					}
				}
				break;
			case DELIVERING_RESOURCES:
				if(ship.getEnergy() < 2500){
					state = ShipStateEnum.GATHERING_ENERGY;
				}else if(ship.getMass() < 300){
					state = ShipStateEnum.GATHERING_RESOURCES;
				}
				break;
			default:
				state = ShipStateEnum.GATHERING_RESOURCES;
		}
	}

}
