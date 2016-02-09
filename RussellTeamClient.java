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
			UUID)
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
			Position))))

/**
 * A team of random agents
 * 
 * The agents pick a random location in space and aim for it.  They shoot somewhat randomly also.
 * @author amy
 *
 */
public class RussellTeamClient extends spacesettlers.clients.TeamClient {
	Prescience prescience;
	
	@Override
	public void initialize(Toroidal2DPhysics space) {
		this.prescience = new Prescience(space);
		prescience.start();
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
		prescience.exit();

	}


	@Override
	public Map<UUID, AbstractAction> 
			getMovementStart(Toroidal2DPhysics space,
					 Set<AbstractActionableObject> actionableObjects) {
				return prescience.getMovementStart(space,actionableObjects);
	}

	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
		prescience.getMovementEnd(space,actionableObjects);
	}

	@Override
	public Set<SpacewarGraphics> getGraphics() {
		return prescience.getGraphics();
	}


	@Override
	/**
	 * Random never purchases 
	 */
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects, 
			ResourcePile resourcesAvailable, 
			PurchaseCosts purchaseCosts) {
		return prescience.getTeamPurchases(space,actionableObjects,resourcesAvailable,purchaseCosts);
	}

	/**
	 * This is the new way to shoot (and use any other power up once they exist)
	 */
	public Map<UUID, SpaceSettlersPowerupEnum> getPowerups(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		return prescience.getPowerups(space,actionableObjects);
	}


}
