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
			
class LibrePD{
	double krv,krp;
	double ktv,ktp;

	LibrePD(double krv, double krp, double ktv, double ktp){
	 /* To be critically damped, the parameters must satisfy:
	 * 2 * sqrt(Kp) = Kv*/
		this.krv = krv;
		this.krp = krp;
		this.ktv = ktv;
		this.ktp = ktp;
	}

	public RawAction getRawAction(Toroidal2DPhysics space, Position position, Position destination, Position aimPoint){
		//Calculate position vectors corresponding to out locations
		Vector2D positionVector = new Vector2D(position);
		Vector2D destinationVector = new Vector2D(destination);
		Vector2D aimPointVector = new Vector2D(aimPoint);

		double shipVelocityX = position.getTranslationalVelocityX();
		double shipVelocityY = position.getTranslationalVelocityY();

		Vector2D shipVelocityVector = new Vector2D(shipVelocityX, shipVelocityY);

		double aimPointVelocityX = aimPoint.getTranslationalVelocityX();
		double aimPointVelocityY = aimPoint.getTranslationalVelocityY();

		Vector2D aimPointVelocityVector = new Vector2D(aimPointVelocityX,aimPointVelocityY);

		double aimPointAngularVelocity = aimPointVelocityVector.getMagnitude() / 
							space.findShortestDistanceVector(position,aimPoint).getMagnitude() *
							Math.sin(aimPointVelocityVector.getAngle() - 
								positionVector.getAngle());

		double shipRotationalVelocity = position.getAngularVelocity();							

		double aimAngle = space.findShortestDistanceVector(position,aimPoint).getAngle();

		double shipAngle = position.getOrientation();

		double angleError = (aimAngle - shipAngle);

		if(angleError > Math.PI){
			angleError -= 2 * Math.PI;
		}else if(angleError < -Math.PI){
			angleError += 2 * Math.PI;

		}


		Vector2D positionError = space.findShortestDistanceVector(position,destination);

		double rotationalAcceleration = 0;
		Vector2D translationalAcceleration = new Vector2D(0,0);

		rotationalAcceleration = angleError * krp + (aimPointAngularVelocity - shipRotationalVelocity) * krv;

		translationalAcceleration.setX(positionError.getXValue()*ktp + (destination.getTranslationalVelocityX() - shipVelocityX)*ktv);
		translationalAcceleration.setY(positionError.getYValue()*ktp + (destination.getTranslationalVelocityY() -shipVelocityY)*ktv);

		RawAction movement = new RawAction(translationalAcceleration,rotationalAcceleration);

		return movement;

	}

}

