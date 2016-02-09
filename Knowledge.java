~(load "macros.scm")

package brya3525;

~(java-import '(	
			(java
				(util
				 	Set
					HashSet))
			(spacesettlers
			 	(objects
				 	Asteroid
					Base
					Beacon
					Ship
					AbstractActionableObject
				 	AbstractObject
					(powerups
					 	PowerupDoubleHealingBaseEnergy
						PowerupDoubleMaxEnergy
						PowerupDoubleWeapon
						PowerupToggleShield
						SpaceSettlersPowerupEnum))
			 	(simulator
				 	Toroidal2DPhysics)
				(utilities
				 	Movement
					Position
					Vector2D))))


public class Knowledge{
	//Our knowledge object holds a copy of everything
	//currently in space. All knowledge will be derived from this object.
	//We need our own allObjects because space doesn't have an accessor for
	//it's allObjects
	Set<AbstractObject> allObjects;
	Toroidal2DPhysics space;
	
	public Knowledge(Toroidal2DPhysics gamespace){
		allObjects = new HashSet<AbstractObject>();
		this.allObjects.addAll(gamespace.getAllObjects());
		this.space=gamespace;
	}

	//This constructor is used for creating smaller knowledge sets
	public Knowledge(Toroidal2DPhysics gamespace, Set<AbstractObject> objects){
		allObjects = new HashSet<AbstractObject>();
		allObjects.addAll(objects);
		space = gamespace;
	}

	public void update(Toroidal2DPhysics gamespace){
		allObjects.clear();
		allObjects.addAll(gamespace.getAllObjects());
		space=gamespace;
	}
	
	//
	//
	//What follows are the functions that don't return a knowledge object;
	//
	//

	public Set<AbstractObject> getObjects(){
		return allObjects;
	}

	public Toroidal2DPhysics getSpace(){
		return space.deepClone();
	}

	public AbstractObject getClosest(Position location){
		double closestDistance = Double.MAX_VALUE;
		AbstractObject closest = null;
		for(AbstractObject obj: allObjects){
			double dist = space.findShortestDistance(location,obj.getPosition());
			if(dist < closestDistance){
				closestDistance = dist;
				closest = obj;
			}
		}

		return closest;
	}


	//
	//Everything from here on returns a knowledge object
	//

	public Knowledge addObjects(Set<AbstractObject> objects){
		allObjects.addAll(objects);
		return this;
	}

	public Knowledge join(Knowledge know){
		return new Knowledge(space).addObjects(know.getObjects());
	}

	
	//Return a set of all objects with
	//minimum distance vectors having magnitude
	//between inner and outer
	public Knowledge getObjectsInRange(Position location, double inner, double outer){
		Set<AbstractObject> objects = new HashSet<AbstractObject>();
		for(AbstractObject object : allObjects){
			double dist = space.findShortestDistance(location,object.getPosition());
			if((dist > inner) &&
			   (dist < outer)){
				objects.add(object);
			   }

		}
		return new Knowledge(space,objects);
	}

	public Knowledge getObjectsInRange(Position location, double outer){
		return new Knowledge(space,getObjectsInRange(location,0,outer).getObjects());
	}

	~(define define-object-accessor
			(java-macro '(object)
			 '(
				 "/*get all instances of " object ".*/\n"
				 "public Knowledge get" object "s(){"
				 	"Set<AbstractObject> " object "s = new HashSet<AbstractObject>();"
					"for( AbstractObject obj : allObjects ){"
						"if( obj instanceof " object "){"
							object "s.add(obj);"
						"}"
					"}"
					"return new Knowledge(space, " object "s);"
				"}\n\n")))

	~(for-each define-object-accessor
			'(
				Asteroid
				Beacon
				Base
				Ship
				))

	public Knowledge getMineableAsteroids(Position location, double inner, double outer){
		Set<AbstractObject> asteroids = getAsteroids().getObjectsInRange(location,inner,outer).getObjects();
		Set<AbstractObject> mineable_asteroids = new HashSet<AbstractObject>();

		for(AbstractObject asteroid : asteroids){
			//this is purely to satisfy the java
			//compiler because it uses static typing
			//rather than type inference. :P
			Asteroid aster = (Asteroid) asteroid;
			if(aster.isMineable()){
				mineable_asteroids.add(aster);
			}
		}

		return new Knowledge(space, mineable_asteroids);

	}


}
