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
			 	Callable))
		 (awt
			Color))
	(brya3525
		TextGraphics
		Knowledge
		Prescience)
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
			Position)
		(clients
		 	ImmutableTeamInfo))))

class SpaceSimulation extends Toroidal2DPhysics implements Callable<SpaceSimulation>{

	SpaceSimulation(Toroidal2DPhysics space, double timeStep ){
		super(space.getHeight(),space.getWidth(),timeStep);
		this.setTeamInfo(new HashSet<ImmutableTeamInfo>(space.getTeamInfo()));
		
		for(AbstractObject obj : space.getAllObjects()){
			AbstractObject newObject = obj.deepClone();
			this.addObject(newObject);
		}
	} 

	public SpaceSimulation call(){
		Map<UUID, SpaceSettlersPowerupEnum> powerups = new HashMap<UUID, SpaceSettlersPowerupEnum>();
		try{
			advanceTime(getCurrentTimestep() + 1, powerups);
		}catch(Exception e){
			e.printStackTrace();
		}
		return this;
	}

}

