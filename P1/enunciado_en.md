# Assignment 1: Ecosystem Simulator

**Objective:** Object-oriented design, and use of generics and collections.

**Due Date:** March 02, 2026, 09:00h

## Copy Control

For each of the TP2 assignments, all the submissions from all the
different TP2 groups will be checked using anti-plagiarism software,
by comparing them all pairwise and by searching to see if any of the
code of any of them is copied from other sources on the Internet
or previous years assignment. Any plagiarism detected will be treated
as a grade of zero for the TP2-course exam session (*convocatoria*) to
which the assignment belongs.

If you decide to store your code in a remote repository, e.g. in a
free version-control system with a view to facilitating collaboration
with your lab partner, make sure your code is not in reach of search
engines. If you are asked to provide your code by anyone other than
your course lecturer, e.g. an employer of a private academy, you must
refuse.

## General Instructions

The following instructions are **strict**, meaning **you must follow them**.

1. Read the entire assignment description before starting.
2. Create a empty Java project in Eclipse (use at least `JDK17`).
3. Download [lib.zip](./lib.zip), unzip it in the project root (at the same level as `src`) and add the libraries to the project. To do this, choose `Project -> Properties -> BuildPath -> Libraries`, check `ClassPath`, press the `Add External Jars` button and select the library you want to add.
4. Download [resources.zip](./resources.zip) and unzip it in the project root (at the same level as `src`).
5. Download [src.zip](./src.zip) and replace the project's `src` directory with this `src` (or copy the content).
6. It is very important that each group member performs points 2-5, because in the exam you will have to do it using the `src.zip` of your assignment (you are not going to use an already assembled project).
7. It is necessary to use exactly the same package structure and class names that appear in the description.
8. Random number generation must be done using `Utils.RAND` (see the section [Random Number Generation](#random-number-generation)). It is forbidden to create another instance of the **Random** class or use **Math.random()**.
9. You must format all code using the Eclipse functionality (`Source->Format`).
10. All constructors must check the validity of parameters and throw corresponding exceptions with an informative message (`IllegalArgumentException` can be used).
11. In the grading, we will take into account code style: use of appropriate names for methods/variables/classes, code formatting indicated in the previous point, etc.
12. When you submit the assignment, upload a file named `src.zip` that includes only the `src` folder. **It is not permitted to call it by another name nor use 7zip, rar, etc.**

## General Description of the Simulator

The simulator aims to simulate an ecosystem composed of animals. Animals in the simulation can be carnivores or herbivores. In this assignment, we have two types: wolves (carnivores) and sheep (herbivores). Each animal is an individual and determines its own behavior based on its environment and its own state. The possible states are: normal state; looking for another animal to mate; fleeing from another animal that is dangerous to it; following another animal to hunt it, etc. When animals mate, other animals can be born inheriting properties from their parents. Animals die when they reach an age limit or run out of energy.

Animals update their states via an `update(double)` method, where the parameter represents a time interval corresponding to a step in the simulation (this must be taken into account when updating age, position, etc.). We use a `Vector2D` class to represent a point on a two-dimensional plane, such as an animal's position (see the section [The Vector2D Class](#the-vector2d-class)).

The simulation includes regions where the animals are located, and the world is composed of a matrix of regions. When moving, animals can travel from one region to another. The goal of the regions is to provide food to the animals when they need it. We will have several types of regions that provide food according to different criteria. Region management (adding animals, removing animals, requesting food, etc.) is done through a region manager.

The main class of the simulation includes a region manager and a list of animals, and allows adding animals to the simulation, advancing the simulation one step, consulting the state, etc. A simulation step includes: removing all dead animals from the simulation; updating the state of all living animals; and giving birth to babies carried by the animals.

The simulator's main loop advances the simulation several steps for `T` seconds (for example, in each step the simulation advances `0.003` seconds – the parameter of the `update` method mentioned above). The loop displays the current state of the animals using the viewer we provide with the assignment (see the section [The Object Viewer](#the-object-viewer)), and additionally, writes the initial and final state of the simulation to a file using the `JSON` format. The initial configuration of the world is loaded from a file in `JSON` format (see the section [Handling JSON in Java](#handling-json-in-java)).

## The Logic of the Simulator (the model)

All classes/interfaces in this section must go in the package `simulator.model`.

The model includes classes to represent animals, regions, region manager, and the main simulator class. The functionality of each class is divided into several interfaces to restrict what different parts of the simulator can do on instances of those classes.

> [!NOTE]
> All numbers used in the formulas below are parameters we have chosen in our implementation of the assignment to achieve reasonable behavior. It is recommended to define them as constants (like `final static`) to be able to test with various values. You can also change the formulas we use and/or the behaviors of the animals in each state, provided you obtain reasonable behavior. See the section [Constants](#constants).

### Common Classes/Interfaces

#### The `JSONable` Interface

Several simulation objects will provide their state in `JSON` format. We define the following interface to implement this functionality:

```java
public interface JSONable {  
  default public JSONObject asJSON() {  
		return new JSONObject();  
	 }  
}

```

#### The `Entity` Interface

Several simulation objects need to update their states in each iteration (in principle, animals and regions). We define the following interface to implement this functionality:

```java
public interface Entity {  
	public void update(double dt);  
}

```

### The Animals

> [!IMPORTANT]
> **It is not permitted to add an attribute for the region in the animal classes. All region management must be done through the region manager.**

#### Diet (Enum)

Animals can be herbivores (`HERBIVORE`) or carnivores (`CARNIVORE`). We define an enumerated type `Diet` with these two values.

#### Animal State (Enum)

Animals can be in one of the following states: normal (`NORMAL`), mating (`MATE`), hungry (`HUNGER`), danger (`DANGER`), or dead (`DEAD`). We define an enumerated type `State` with these `5` values.

#### The `AnimalInfo` Interface

It is an interface that defines what information can be seen about an animal. It includes methods that query the animal's state but never modify it:

```java
public interface AnimalInfo extends JSONable { // Note that it extends JSONable  
	public State getState();  
	public Vector2D getPosition();  
	public String getGeneticCode();  
	public Diet getDiet();  
	public double getSpeed();  
	public double getSightRange();  
	public double getEnergy();  
	public double getAge();
	public Vector2D getDestination();  
	public boolean isPregnant();  
}

```

When we want to pass an instance of the `Animal` class to a part of the program that cannot alter the state, we will pass it as `AnimalInfo`. More methods can be added if necessary, provided they **do not alter** the animal's state.

#### Animal Selection Strategies

In some circumstances, animals will have to choose an animal from a list of animals (that are within their visual field). For example, to mate, to search for a hunting target, etc. So that animals can have different selection behaviors (even if the animals are of the same type), we are going to use selection strategies. We will use the following interface to represent a selection strategy:

```java
public interface SelectionStrategy {  
	Animal select(Animal a, List<Animal> as);  
}

```

The `select` method selects for animal `a` an animal from the list `as` according to the concrete strategy's criteria (it is assumed that the object corresponding to animal `a` will invoke select). If the list is empty, it always returns `null`. It is assumed that `a` does not appear in the list `as`.

Implement the following strategies:

* `SelectFirst`: returns the first animal in the list `as`.
* `SelectClosest`: returns the animal closest to animal `a` from the list `as`.
* `SelectYoungest`: returns the youngest animal from the list `as`.

You can implement more strategies if you wish.

#### The `Animal` Class

We represent an animal with the *abstract class* `Animal` which implements the interfaces `Entity` and `AnimalInfo`. Below we will define `2` types of animals that inherit from this class.

##### Necessary Attributes

Each animal must carry at least the following attributes (can be `protected` to be able to access directly from subclasses or `private` and define corresponding `getters`):

* `geneticCode` (`String`): is a non-empty character string representing the genetic code. Each subclass will assign a different value to this field. In principle, it is used to know if 2 animals can mate or not (e.g., if they have the same genetic code).
* `diet` (`Diet`): indicates if the animal is an herbivore or carnivore.
* `state` (`State`): the current state of the animal.
* `pos` (`Vector2D`): the position of the animal.
* `dest` (`Vector2D`): the destination of the animal (the animal always has a destination, and when it reaches it, it chooses another, or changes it depending on whether it is following another animal or being chased by another animal).
* `energy` (`double`): the energy of the animal. When it reaches `0.0` the animal dies.
* `speed` (`double`): the speed of the animal.
* `age` (`double`): the age of the animal. When it reaches a maximum (depending on the type of animal) the animal dies.
* `desire` (`double`): the desire of the animal, which changes during the simulation. We will use it to decide if an animal enters (or exits) a mating state.
* `sightRange` (`double`): the radius of the animal's visual field (to decide which animals it can see).
* `mateTarget` (`Animal`): a reference to an animal with which it wants to mate.
* `baby` (`Animal`): a reference indicating if the animal is carrying a baby that hasn't been born yet.
* `regionMngr` (`AnimalMapView`): is the region manager to be able to query information or perform corresponding operations (see the section [The Region Manager](#the-region-manager)). When we create the object, we initialize them to `null`, until the region manager initializes the animal by calling its `init` method.
* `mateStrategy` (`SelectionStrategy`): is the selection strategy to search for a partner.

##### Constructors

It is necessary to have `2` constructors. The first is used to create initial objects and the second for when an animal is born.

*The first constructor* is as follows (more parameters can be added if necessary)

```java
protected Animal(String geneticCode, Diet diet, double sightRange, double initSpeed, SelectionStrategy mateStrategy, Vector2D pos)

```

where `geneticCode` must be a non-empty character string, `sightRange` and `initSpeed` positive numbers and `mateStrategy` is not `null`. A corresponding exception with an informative message must be thrown if any value is incorrect (e.g., `IllegalArgumentException`).

The values of `geneticCode`, `diet`, `sightRange`, `pos`, and `mateStrategy` are initialized to the received values. Initialize `speed` to `Utils.getRandomizedParameter(initSpeed, 0.1)` — see this method in the `Utils` class. Note that the value of `pos` can be `null` and in that case, it is initialized to a random value in the `init` method (not in the constructor, see the description of the `init` method).

Apart from the received values, the other attributes must be initialized as follows: `state` is `NORMAL`, `energy` is `100.0`, `desire` is `0.0`, and `dest`, `mateTarget`, `baby`, and `regionMngr` are `null`.

*The second constructor* is used for when an animal is born from `2` others:

```java
protected Animal(Animal p1, Animal p2)

```

Attributes must be initialized as follows: `dest`, `baby`, `mateTarget`, and `regionMngr` are `null`, `state` is `NORMAL`, `desire` is `0.0`, `geneticCode` and `diet` are inherited from `p1`, `mateStrategy` is inherited from `p2`, `energy` is the average of the energies of `p1` and `p2`, `pos` is a random position near `p1` using for example:

```java
p1.getPosition().plus(Vector2D.getRandomVector(-1,1).scale(60.0*(Utils.RAND.nextGaussian()+1)))

```

`sightRange` is a mutation of the average of the visual fields of `p1` and `p2` using for example:

```java
Utils.getRandomizedParameter((p1.getSightRange()+p2.getSightRange())/2, 0.2)

```

`speed` is a mutation of the average of the speeds of `p1` and `p2` using for example:

```java
Utils.getRandomizedParameter((p1.getSpeed()+p2.getSpeed())/2, 0.2)

```

##### Necessary Methods

In addition to the methods of the interface it implements, the following methods must be implemented:

* `void init(AnimalMapView regMngr)`: the region manager will invoke this method when adding the animal to the simulation:
	* Initialize `regionMngr` to `regMngr`.
	* If `pos` is `null`, a random position must be chosen within the map range (`X` between `0` and `regionMngr.getWidth()-1` and `Y` between `0` and `regionMngr.getHeight()-1`). If `pos` is not `null`, it must be adjusted so that it is within the map if necessary (see the section [Adjusting positions](#adjusting-positions)).
	* Choose a random position for `dest` (within the map range).


* `Animal deliverBaby()`: return `baby` and set it to `null`. The simulator will invoke this method so that animals are born.
* `protected void move(double speed)`: subclasses use this method to update the animal's position (so that it moves towards dest with speed `speed`). This can be done using

	```java
	pos = pos.plus(dest.minus(pos).direction().scale(speed))

	```


* `protected void setState(State state)`: changes the value of the `state` attribute to `state` and calls a corresponding method, depending on the state, to carry out some complementary action. It can be like the following, which simply calls an abstract method according to the state (see description of abstract methods below):

	```java
	protected void setState(State state) {
    	this.state = state;
	    switch (state) {
	    case NORMAL:
	        setNormalStateAction();
	        break;
	    case HUNGER:
	        // ...
	    }
	}

	```


* `abstract protected void setNormalStateAction()`: implemented in subclasses to execute an action complementary to the corresponding state change (e.g., in the `Sheep` class it sets `mateTarget` and `dangerSource` to `null` -- see the [`Sheep`](#the-sheep-class) class).
* `abstract protected void setMateStateAction()`: implemented in subclasses to execute an action complementary to the corresponding state change (e.g., in the `Sheep` class it sets `dangerSource` to `null` -- see the [`Sheep`](#the-sheep-class) class).
* `abstract protected void setHungerStateAction()`: implemented in subclasses to execute an action complementary to the corresponding state change (e.g., in the `Wolf` class it sets `mateTarget` to `null` -- see the [`Wolf`](#the-wolf-class) class).
* `abstract protected void setDangerStateAction()`: implemented in subclasses to execute an action complementary to the corresponding state change (e.g., in the `Sheep` class it sets `mateTarget` to `null` -- see the [`Sheep`](#the-sheep-class) class).
* `abstract protected void setDeadStateAction()`: implemented in subclasses to execute an action complementary to the corresponding state change (e.g., in the `Sheep` class it sets `mateTarget` and `dangerSource` to `null` -- see the [`Sheep`](#the-sheep-class) class).
* `public JSONObject asJSON()`: returns a `JSON` structure like the following:

   ```json
   {
     "pos": [28.90696391797469,22.009772194487613],
     "gcode": "Sheep",
     "diet": "HERBIVORE",
     "state": "NORMAL"
   }
   ```

#### The `Sheep` Class

It is a class representing a sheep. It is an herbivorous animal with genetic code `"Sheep"`. It is an animal that does not hunt other animals, only eats what the region it is in provides, and can mate with other animals with the same genetic code.

##### Necessary Attributes

It is necessary to maintain a reference (`dangerSource`) to another animal that is considered a danger at a given moment, and another reference (`dangerStrategy`) to a selection strategy to choose a danger from the list of animals in the visual field.

##### Constructors

Its *first constructor*

```java
public Sheep(SelectionStrategy mateStrategy, SelectionStrategy dangerStrategy,  Vector2D pos)

```

receives the strategies and the position and stores them in the corresponding attributes (calling the superclass constructor). The initial visual field is `40.0` and the initial speed is `35.0`.

Its *second constructor* is used when an animal of type `Sheep` is born:

```java
protected Sheep(Sheep p1, Animal p2)

```

Aside from calling the corresponding superclass constructor, it must inherit `dangerStrategy` from `p1` and set its `dangerSource` to `null`.

##### Necessary Methods

It is necessary to implement the `update` method. This method must do the following:

1. If the state is `DEAD` do nothing (return immediately).
2. Update the object according to the animal's state (see the description below).
3. If the position is outside the map, adjust it and change its state to `NORMAL`.
4. If `energy` is `0.0` or `age` is greater than `8.0`, change its state to `DEAD`.
5. If its state is not `DEAD`, it asks for food from the region manager using `getFood(this, dt)` and adds it to its `energy` (always keeping it between `0.0` and `100.0`).

*To search for an animal considered dangerous*, one must ask the region manager for the list of **carnivorous** animals in the visual field, using the `getAnimalsInRange` method, and then choose one using the corresponding selection strategy.

*To search for an animal to mate with*, one must ask the region manager for the list of animals **with the same genetic code** in the visual field, using the `getAnimalsInRange` method, and then choose one using the corresponding selection strategy.

In addition to what we explain below remember that: when changing to `NORMAL` state it must set `dangerSource` and `mateTarget` to `null`; when changing to `MATE` state it must set `dangerSource` to `null`; and when changing to `DANGER` it must set `mateTarget` to `null`.

> [!IMPORTANT]
> In point 2 above you must use a `switch` to distinguish the different states and call other methods to carry out the corresponding action. You cannot include all the code in this method.

##### How to Update the Object According to State

Flowchart of the steps detailed below: [[svg](./sheep_flow_chart.svg)] [[png](./sheep_flow_chart.png)]

If the current state is `NORMAL`:

1. Advance the animal according to the following steps:
	1. If the distance from the animal to the destination (`dest`) is less than `8.0`, choose another destination randomly (within the map dimensions).
	2. Advance (calling `move`) with speed `speed*dt*Math.exp((energy-100.0)*0.007)`.
	3. Add `dt` to the age.
	4. Subtract `20.0*dt` from the energy (always keeping it between `0.0` and `100.0`).
	5. Add `40.0*dt` to the desire (always keeping it between `0.0` and `100.0`).
2. State Change
	1. If `dangerSource` is `null`, look for a new animal considered dangerous.
	2. If `dangerSource` is not `null`, change state to `DANGER`, and if it is `null` and desire greater than `65.0` change state to `MATE`.

If the current state is `DANGER`:

1. If `dangerSource` is not `null` and its state is `DEAD`, set `dangerSource` to `null` because it has already died for some reason and is no longer dangerous.
2. If `dangerSource` is `null`, advance normally like point 1 of the `NORMAL` case above, and if `dangerSource` is not `null`:
	1. We want to change the destination to move in the opposite direction of the danger. This can be done with `pos.plus(pos.minus(dangerSource.getPosition()).direction())` as the destination.
	2. Advance (calling `move`) with speed `2.0*speed*dt*Math.exp((energy-100.0)*0.007)`.
	3. Add `dt` to the age.
	4. Subtract `20.0*1.2*dt` from the energy (always keeping it between `0.0` and `100.0`).
	5. Add `40.0*dt` to the desire (always keeping it between `0.0` and `100.0`).
3. State Change
	1. If `dangerSource` is `null` or `dangerSource` is not in the animal's visual field
		1. Search for a new animal considered as danger.
		2. If `dangerSource` is `null`:
			1. If desire is less than `65.0`, change state to `NORMAL`, otherwise change it to `MATE`.

If the current state is `MATE`:

1. If `mateTarget` is not `null` and its state is `DEAD` or is outside the visual field, set `mateTarget` to `null` since it will not follow it to mate.
2. If `mateTarget` is `null`, search for an animal to mate with and if one is not found advance normally like point 1 of the `NORMAL` case above; in the other case (where `mateTarget` is no longer `null`):
	1. We want to change the destination to pursue `mateTarget`. This can be done by changing it to `mateTarget.getPosition()`.
	2. Advance (calling `move`) with speed `2.0*speed*dt*Math.exp((energy-100.0)*0.007)`.
	3. Add `dt` to the age.
	4. Subtract `20.0*1.2*dt` from the energy (always keeping it between `0.0` and `100.0`).
	5. Add `40.0*dt` to the desire (always keeping it between `0.0` and `100.0`).
	6. If the distance from the animal to `mateTarget` is less than `8.0`, then they will mate according to the following steps:
		1. Reset the desire of the animal and the `mateTarget` to `0.0`.
		2. If the animal is not already carrying a baby, with a probability of `0.9` it will carry a new baby using `new Sheep(this, mateTarget)`.
		3. Set `mateTarget` to `null`.
3. If `dangerSource` is `null` search for a new animal considered as dangerous.
4. If `dangerSource` is not `null` change state to `DANGER`, and if it is `null` and desire is less than `65.0` change state to `NORMAL`.

If the current state is `HUNGER`:

A Sheep object can never be in `HUNGER` state.

#### The `Wolf` Class

It is a class representing a wolf. It is a carnivorous animal with genetic code `"Wolf"`. It is an animal that hunts other herbivorous animals and can also eat what the region it is in provides, and can mate with other animals with the same genetic code.

##### Necessary Attributes

It is necessary to maintain a reference (`huntTarget`) to another animal it wants to hunt at a given moment, and another reference (`huntingStrategy`) to a selection strategy to choose an animal to hunt.

##### Constructors:

Its *first constructor*

```java
public Wolf(SelectionStrategy mateStrategy, SelectionStrategy huntingStrategy,  Vector2D pos)

```

receives the strategies and the position and simply stores them in the corresponding attributes (calling the superclass constructor). The initial visual field is `50.0` and the initial speed is `60.0`.

Its *second constructor* is used when an animal of type `Wolf` is born:

```java
protected Wolf(Wolf p1, Animal p2)

```

Aside from calling the corresponding superclass constructor, it must inherit `huntingStrategy` from `p1` and set its `huntTarget` to `null`.

##### Necessary Methods

It is necessary to implement the `update` method. This method must do the following:

1. If the state is `DEAD` do nothing (return immediately).
2. Update the object according to the animal's state (see the description below)
3. If the position is outside the map, adjust it and change its state to `NORMAL`.
4. If `energy` is `0.0` or `age` is greater than `14.0`, change its state to `DEAD`.
5. If its state is not `DEAD`, ask for food from the region manager using `getFood(this, dt)` and add it to its `energy` (always keeping it between `0.0` and `100.0`).

*To search for an animal to hunt*, one must ask the region manager for the list of **herbivorous** animals in the visual field, using the `getAnimalsInRange` method, and then choose one using the corresponding selection strategy.

*To search for an animal to mate with*, one must ask the region manager for the list of animals **with the same genetic code** in the visual field, using the `getAnimalsInRange` method, and then choose one using the corresponding selection strategy.

In addition to what we explain below remember that: when changing to `NORMAL` state it must set `huntTarget` and `mateTarget` to `null`; when changing to `MATE` state it must set `huntTarget` to `null`; when changing to `HUNGER` state it must set `mateTarget` to `null`.

> [!IMPORTANT]
> In point 2 above you must use a `switch` to distinguish the different states and call other methods to carry out the corresponding action. You cannot include all the code in this method.

##### How to Update the Object According to State

Flowchart of the steps detailed below: [[svg](./wolf_flow_chart.svg)] [[png](./wolf_flow_chart.png)]

If the current state is `NORMAL`:

1. Advance the animal according to the following steps:
	1. If the distance from the animal to the destination (`dest`) is less than `8.0`, choose another destination randomly (within the map dimensions).
	2. Advance (calling `move`) with speed `speed*dt*Math.exp((energy-100.0)*0.007)`.
	3. Add `dt` to the age.
	4. Subtract `18.0*dt` from the energy (always keeping it between `0.0` and `100.0`).
	5. Add `30.0*dt` to the desire (always keeping it between `0.0` and `100.0`).
2. State Change
	1. If its energy is less than `50.0` change state to `HUNGER`, and if it is not and its desire is greater than `65.0` change state to `MATE`. Otherwise, do nothing.

If the current state is `HUNGER`:

1. If `huntTarget` is `null`, or is not null but its state is `DEAD` or is outside the visual field, search for another animal to hunt.
2. If `huntTarget` is `null`, advance normally like point `1` of the `NORMAL` case above, and if `huntTarget` is not `null`:
	1. We want to change the destination to advance towards the animal it wants to hunt. This can be done with `huntTarget.getPosition()` as the destination.
	2. Advance (calling `move`) with speed `3.0*speed*dt*Math.exp((energy-100.0)*0.007)`.
	3. Add `dt` to the age.
	4. Subtract `18.0*1.2*dt` from the energy (always keeping it between `0.0` and `100.0`).
	5. Add `30.0*dt` to the desire (always keeping it between `0.0` and `100.0`).
	6. If the distance from the animal to `huntTarget` is less than `8.0`, then it will hunt according to the following steps:
		1. Set the state of `huntTarget` to `DEAD`.
		2. Set `huntTarget` to `null`.
		3. Add `50.0` to the energy (always keeping it between `0.0` and `100.0`).
3. Change State
	1. If its energy is greater than `50.0`
	2. If desire is less than `65.0` change state to `NORMAL`, otherwise change it to `MATE`.

If the current state is `MATE`:

1. If `mateTarget` is not `null` and its state is `DEAD` or is outside the visual field, set `mateTarget` to `null` since it will not follow it to mate.
2. If `mateTarget` is `null`, search for an animal to mate with and if one is not found advance normally like point 1 of the `NORMAL` case above; in the other case (`mateTarget` was no longer `null`):
	1. We want to change the destination to pursue `mateTarget`, this can be done with `mateTarget.getPosition()` as the destination.
	2. Advance (calling `move`) with speed `3.0*speed*dt*Math.exp((energy-100.0)*0.007)`.
	3. Add `dt` to the age.
	4. Subtract `18.0*1.2*dt` from the energy (always keeping it between `0.0` and `100.0`).
	5. Add `30.0*dt` to the desire (always keeping it between `0.0` and `100.0`).
	6. If the distance from the animal to `mateTarget` is less than `8.0`, then they will mate according to the following steps:
		1. Reset the desire of the animal and of `mateTarget` to `0.0`.
		2. If the animal is not already carrying a baby, with a probability of `0.9` it will carry a new baby using `new Wolf(this, mateTarget)`.
		3. Subtract `10.0` from the energy (always keeping it between `0.0` and `100.0`).
		4. Set `mateTarget` to `null`.
3. If its energy is less than `50.0` change state to `HUNGER`, and if it is not and desire is less than `65.0` change state to `NORMAL`.

If the current state is `DANGER`:

A Wolf object can never be in `DANGER` state.

### Regions

Regions are zones where animals are found. Regions provide food according to different criteria.

#### The `FoodSupplier` Interface

We use the following interface to ask for food for animal `a` during `dt` seconds:

```java
public interface FoodSupplier {  
	double getFood(AnimalInfo a, double dt);  
}

```

#### The `RegionInfo` Interface

It is an interface that defines what information can be seen about a region and includes methods that query the state of the region but never modify it. For now, the interface only extends `JSONable`, later we will add more methods.

```java
public interface RegionInfo extends JSONable {  
	// for now it is empty, later we will make it implement the interface  
	// Iterable<AnimalInfo>  
}

```

#### The `Region` Class

We represent a region with the abstract class Region which implements the interfaces `Entity`, `FoodSupplier`, and `RegionInfo`.

##### Necessary Attributes

An attribute with the list of animals found in the region. Keep it protected to be able to access directly from subclasses.

##### Constructors

It has only one default constructor that initializes the list of animals.

##### Necessary Methods

In addition to the methods of the interface it implements, the following methods must be implemented:

* `final void addAnimal(Animal a)`: adds animal `a` to the list of animals.

* `final void removeAnimal(Animal a)`: removes the animal from the list of animals.

* `final List<Animal> getAnimals()`: returns an **unmodifiable** version of the list of animals.

* `public JSONObject asJSON()`: returns a `JSON` structure like the following where `ai` is what `asJSON()` of the corresponding animal returns:
	```json
	{  
  	   "animals": [a1,a2,...],  
	}

	```

#### The `DefaultRegion` Class

The `DefaultRegion` class represents a region that gives food only to herbivorous animals. It has no constructors (only the default constructor, which is defined automatically).

Its `getFood(a,dt)` method returns `0.0` if the animal asking for food is carnivorous, and the following if it is herbivorous, where `n` is the number of herbivorous animals in the region:

```java
60.0*Math.exp(-Math.max(0,n-5.0)*2.0)*dt

```

Its `update` method does nothing.

#### The `DynamicSupplyRegion` Class

The `DynamicSupplyRegion` class represents a region that gives food only to herbivorous animals, but the amount of food can decrease/increase. Its constructor receives the initial amount of food (positive number of type `double`) and a growth factor (non-negative number of type `double`).

Its `getFood(a,dt)` method returns `0.0` if the animal asking for food is carnivorous, and the following if it is herbivorous, where `n` is the number of herbivorous animals in the region and `food` is the current amount of food:

```java
Math.min(food,60.0*Math.exp(-Math.max(0,n-5.0)*2.0)*dt)

```

In addition, it subtracts the returned value from the amount of food `food` that the region currently has. Its `update` method increments, with probability `0.5`, the amount of food by `dt*factor` where `factor` is the growth factor.

### The Region Manager

The region manager is a class that facilitates working with regions. It represents a map with fixed width and height and maintains a matrix of regions (with fixed numbers of columns and rows). One can both register an animal (and place it in its region) and remove an animal (and remove it from its region). An animal can ask the manager for food and the manager delegates the request to the corresponding region, etc.

#### The `MapInfo` Interface

It is an interface that represents the map and allows us to query information but never modify the map.

```java
public interface MapInfo extends JSONable {  
	public int getCols();  
	public int getRows();  
	public int getWidth();  
	public int getHeight();  
	public int getRegionWidth();  
	public int getRegionHeight();  
}

```

#### The `AnimalMapView` Interface

It is an interface that represents what an animal can see of the region manager. In principle, it can see the map, ask for food, and can also ask for the list of animals in its visual field that also satisfy some condition.

```java
public interface AnimalMapView extends MapInfo, FoodSupplier {  
	public List<Animal> getAnimalsInRange(Animal e, Predicate<Animal> filter);  
}

```

#### The `RegionManager` Class

We represent the region manager with the `RegionManager` class which implements the `AnimalMapView` interface.

##### Necessary Attributes

It must maintain basic information about the map (map width/height, columns, rows, width/height of a region). Additionally, it must maintain a matrix of regions with corresponding number of rows and columns (`regions`), and a map (`animalRegion`) of type `Map<Animal, Region>` that assigns each animal its current region.

##### Constructors

There is only one constructor that receives the number of columns, the number of rows, the width, and the height.

```java
public RegionManager(int cols, int rows, int width, int height)

```

It must store the parameters in the corresponding attributes, and calculate the width and height of a cell (dividing total width/height by the number of columns/rows) and store it in the corresponding attributes. Additionally, it must initialize the `regions` matrix with regions of type `DefaultRegion` (using the default constructor) and initialize `animalRegion` with a suitable data structure.

##### Necessary Methods

In addition to the methods of the interface it implements, the following methods must be implemented:

* `void setRegion(int row, int col, Region r)`: modifies the region located at column `row` and row `col` to `r`. Additionally adds all animals that were in the previous region to `r` and updates their entries in `animalRegion`.

* `void registerAnimal(Animal a)`: finds the region to which the animal should belong (based on its position) and adds it to that region and updates `animalRegion`. Additionally, calls the `init` method passing a reference to itself (the region manager).

* `void unregisterAnimal(Animal a)`: removes the animal from the region to which it belongs and updates `animalRegion`.

* `void updateAnimalRegion(Animal a)`: finds the region to which the animal should belong (based on its current position), and if it is different from its current region adds it to the new region, removes it from the previous one, and updates `animalRegion`.

* `public double getFood(AnimalInfo a, double dt)`: calls `getFood` of the region to which the animal belongs and returns the corresponding value.

* `void updateAllRegions(double dt)`: calls `update` of all regions in the regions matrix.

* `public List<Animal> getAnimalsInRange(Animal a, Predicate<Animal> filter)`: returns a list of all animals that are in the visual field of animal `a` and meet the `filter` condition. It must consult only the regions in the visual field.

* `public JSONObject asJSON()`: returns a `JSON` structure in the following form

	```json
  	{
      "regions": [o1,o2,...]
  	}
	```

    where `oi` is a JSON structure corresponding to a region and has the following form

	```json
 	{  
      "row": i,
   	  "col": j,
      "data": r
	}
	```

	where `r` is what `asJSON()` returns from the region in row `i` and column `j`.

### The `Simulator` Class

This is the main class of the model, through which we can add animals, modify regions, and advance the simulation. We represent it with the `Simulator` class which implements the `JSONable` interface.

#### Necessary Attributes

This class needs to receive as attributes a factory for animals and another for regions (see the section [The Factories](#the-factories)). Additionally, it needs to carry a region manager, a list with all animals participating in the simulation, and the current time (`double`).

#### Constructors

It has only one constructor, which receives the dimensions and factories:

```java
public Simulator(int cols, int rows, int width, int height,   
                  Factory<Animal> animalsFactory, Factory<Region> regionsFactory)

```

The constructor stores the parameters in corresponding attributes, creates the region manager and the animal list, and initializes the time to `0.0`.

#### Necessary Methods

The following methods must be implemented:

* `private setRegion(int row, int col, Region r)`: adds region `r` to the region manager at position `(row,col)`.

* `void setRegion(int row, int col, JSONObject rJson)`: creates a region `R` from `rJson` and calls `setRegion(row,col,R)`.

* `private void addAnimal(Animal a)`: adds animal `a` to the animal list and registers it in the region manager.

* `public void addAnimal(JSONObject aJson)`: creates an animal `A` from `aJson` and calls `addAnimal(A)`.

* `public MapInfo getMapInfo()`: returns the region manager.

* `public List<? extends AnimalInfo> getAnimals()`: returns an **unmodifiable** version of the animal list.

* `public double getTime()`: returns the current time.

* `public void advance(double dt)`: advances the simulation one step. Care must be taken not to modify the animal list while iterating over it. The following steps must be followed (**the order of steps is very important**):
	1. Increment the time by `dt`.
	2. Remove all animals with `DEAD` state from the animal list and eliminate them from the region manager.
	3. For each animal: call its `update(dt)` and ask the region manager to update its region.
	4. Ask the region manager to update all regions.
	5. For each animal: if `isPregnant()` returns `true`, obtain the baby using its `deliverBaby()` method and add it to the simulation using `addAnimal`.

* `public JSONObject asJSON()`: returns a `JSON` structure like the following, where `t` is the current time and `s` is what `asJSON()` of the region manager returns:

	```json
  	{
   	  "time": t,
   	  "state": s
  	}
	```

> [!NOTE]
> As you can observe, there are two versions of the `addAnimal` and `setRegion` methods, some receive the input as `JSON` while others receive the corresponding objects after creating them. Those that receive the objects are `private`. The goal of having the 2 versions is to facilitate the development and debugging of the assignment: in your first implementation, before implementing the factories, change those methods from `private` to `public` and use them directly to add animals and regions from the outside. Only when you implement the factories change them to `private` again. This way you can debug the program without having implemented the factories.

## The `Controller`

All classes/interfaces in this section must go in the package `simulator.control`.

The controller is implemented in the `Controller` class which is responsible for (1) extracting animal/region specifications from a `JSONObject` and adding them to the simulator; (2) running the simulator for a determined time and writing the different initial and final states to a given `OutputStream`.

### Necessary Attributes

It must have an attribute (`sim`) for the `Simulator` instance.

### Constructors

The only constructor receives an object of type `Simulator` as a parameter and stores it in the corresponding attribute.

```java
public Controller(Simulator sim)

```

### Necessary Methods

* `public void loadData(JSONObject data)`: we assume that data has the two keys `"animals"` and `"regions"`, the latter being optional. The values of these keys are of type `JSONArray` (list) and each element of the list is a `JSONObject` corresponding to an animal or region specification. For each element, the following must be done (**it is very important to add the regions before adding the animals**):

* Each `JSONObject` in the list of regions (if any, because it is optional) has the form

	```json
 	{"row": [rf, rt], "col": [cf, ct], "spec": O} 
	```

	where `rf`, `rt`, `cf`, and `ct` are integers and `O` is a `JSONObject` describing a region (see the section [The Factories](#the-factories)). You must call `sim.setRegion(R,C,O)` for each `rf ≤ R ≤ rt` and `cf ≤ C ≤ ct` (i.e. using a nested loop to modify multiple regions).

* Each `JSONObject` in the list of animals has the form

	```json
  	{"amount": N, "spec": O} 
	```

	where `N` is a positive integer and `O` is a `JSONObject` describing an animal (see the section [The Factories](#the-factories)). You must call `sim.addAnimal(O)` in a loop `N` times to add `N` animals of this type.

* `public void run(double t, double dt, boolean sv, OutputStream out)`: is a method to execute the simulator (in a loop) calling `sim.advance(dt)` until `t` seconds pass (i.e. until `sim.getTime()>t`). Additionally, it must write to `out` a `JSON` structure of the following form:

	```json
  	{
   	  "in": initState,
   	  "out": finalState
  	}
	```

	Where `initState` is the result returned by `sim.asJSON()` before entering the loop, and `finalState` is the result returned by `sim.asJSON()` upon exiting the loop.

	Additionally, if the value of `sv` is `true`, the simulation must be shown using the object viewer (see the section [The Object Viewer](#the-object-viewer)).

## The Factories

All classes/interfaces in this section must go in the package `simulator.factories`.

As we have several factories in the assignment, we are going to use generics to avoid duplicating code. Below we detail how to implement them step by step.

### The `Factory<T>` Interface

A factory is modeled with the generic interface `Factory<T>`:

```java
public interface Factory<T> {  
	public T createInstance(JSONObject info);  
	public List<JSONObject> getInfo();  
}

```

The `createInstance` method receives a `JSON` structure describing the object to create, and returns an instance of the corresponding class -- an instance of a subtype of `T`. In case `info` is incorrect, it throws the corresponding exception. In our case, the `JSON` structure passed as a parameter to the `createInstance` method includes two keys:

* `"type"`: is a string describing the object to be created;
* `"data"`: is a `JSON` structure that includes all the information necessary to create the object, for example, the necessary arguments in the corresponding class constructor, etc.

The `getInfo` method returns a list of `JSON` objects describing what can be created by the factory, see details below.

There are many ways to define a factory, which we will see during the course (or in the Software Engineering course). We will design it using what is known as a *builder based factory*, which allows extending a factory with more options without modifying its code. It is a combination of the Command and Factory design patterns.

### The `Builder<T>` Class

The basic element in a *builder based factory* is the builder, which is a class capable of creating an instance of a specific type. We can model it as a generic class `Builder<T>`:

```java
public abstract class Builder<T> {  
	private String typeTag;  
	private String desc;

	public Builder(String typeTag, String desc) {
    if (typeTag == null || desc == null || typeTag.isBlank() || desc.isBlank())  
			throw new IllegalArgumentException("Invalid type/desc");  
		this.typeTag = typeTag;  
		this.desc = desc;
	}

	public String getTypeTag() {  
		return typeTag;  
	}

	public JSONObject getInfo() {  
		JSONObject info = new JSONObject();  
		info.put("type", typeTag);  
		info.put("desc", desc);  
		JSONObject data = new JSONObject();  
		fillInData(data);
		info.put("data", data);  
		return info;  
	}

	protected void fillInData(JSONObject o) {  
	}

	@Override  
	public String toString() {  
		return desc;  
	}

	protected abstract T createInstance(JSONObject data);  
}

```

The `typeTag` attribute coincides with the `"type"` field of the corresponding `JSON` structure, and the `desc` attribute describes what type of objects can be created by this builder (to show the user when necessary). Subclasses must override `fillInData` to fill parameters in `"o"` with a description of the different parts of the corresponding `JSON` structure (to show the user when necessary).

Classes extending `Builder<T>` are responsible for assigning a value to `typeTag` by calling the `Builder<T>` class constructor, and also for defining the `createInstance` method to create an object of type `T` (or any instance that is a subclass of `T`) in case all necessary information is available in `data`. Otherwise, it generates an exception of type `IllegalArgumentException` describing what information is incorrect or unavailable.

The `getInfo` method returns a `JSON` object with two fields corresponding to `typeTag` and `desc`, which will be used by the `getInfo()` method of the factory. If we want to add more information we have to override `fillInData` to fill it.

Use the `Builder<T>` class to define the following concrete *builders*:

* `SelectFirstBuilder`
* `SelectClosestBuilder`
* `SelectYoungestBuilder`
* `SheepBuilder`
* `WolfBuilder`
* `DefaultRegionBuilder`
* `DynamicSupplyRegionBuilder`

Below you can find the corresponding `JSON` admitted by each builder. All *builders* must throw exceptions when input data is not valid (use `IllegalArgumentException` with an informative message).

#### `JSON` structures admitted by Builders

**SelectFirstBuilder**

```json
{  
  "type": "first", 
  "data": {}  
}

```

**SelectClosestBuilder**

```json
{  
  "type": "closest",
  "data": {}
}

```

**SelectYoungestBuilder**

```json
{  
  "type": "youngest",
  "data": {}  
}

```

**SheepBuilder**

```json
{
  "type": "sheep",
  "data": {
    "mate_strategy": { … },
    "danger_strategy": { … },
    "pos": {
      "x_range": [100.0, 200.0],
      "y_range": [100.0, 200.0]
    }
  }
}

```

The value of the key `"mate_strategy"` is a `JSON` of a strategy that must be constructed using the strategy factory. It is optional, and if it does not exist, use the `SelectFirst` strategy.

The value of the key `"danger_strategy"` is a `JSON` of a strategy that must be constructed using the strategy factory. It is optional, and if it does not exist, use the `SelectFirst` strategy.

The key `"pos"` is optional, if it does not exist use `null`. If it exists, an initial position must be chosen where the `X` coordinate is within the range `"x_range"` and the Y coordinate is within the range `"y_range"`.

Note that this builder requires access to a strategy factory (must be passed as a parameter to the constructor).

**WolfBuilder**

```json
{
  "type": "wolf",
  "data": {
    "mate_strategy" : { … },
    "hunt_strategy" : { … },
    "pos" : {
      "x_range" : [100.0, 200.0],
      "y_range" : [100.0, 200.0]
    }
  }
}

```

The value of the key `"mate_strategy"` is a `JSON` of a strategy that must be constructed using the strategy factory. It is optional, and if it does not exist, use the `SelectFirst` strategy.

The value of the key `"hunt_strategy"` is a `JSON` of a strategy that must be constructed using the strategy factory. It is optional, and if it does not exist, use the `SelectFirst` strategy.

The key `"pos"` is optional, if it does not exist use `null`. If it exists, an initial position must be chosen where the `X` coordinate is within the range `"x_range"` and the `Y` coordinate is within the range `"y_range"`.

Note that this builder requires access to a strategy factory (must be passed as a parameter to the constructor).

**DefaultRegionBuilder**

```json
{  
  "type" : "default",  
  "data" : { }  
}

```

**DynamicSupplyRegionBuilder**

```json
{
  "type" : "dynamic",
  "data" : {
     "factor" : 2.5,
     "food" : 1250.0
   }
}

```

The key `"factor"` is optional with a default value of `2.0`. The key `"food"` is optional with a default value of `1000.0`.

### The `BuilderBasedFactory<T>` Class

Once the *builders* are prepared, we implement a generic *builder based factory*. It is a class that has a map of *builders*, such that when we want to create an object from a `JSON` structure, it finds the builder with which to generate the corresponding instance:

```java
public class BuilderBasedFactory<T> implements Factory<T> {  
	private Map<String, Builder<T>> builders;  
	private List<JSONObject> buildersInfo;

	public BuilderBasedFactory() {  
      // Create a HashMap for builders, and a LinkedList buildersInfo  
      // …  
	}

	public BuilderBasedFactory(List<Builder<T>> builders) {  
		this();

       // call addBuilder(b) for each builder b in builder  
       // …  
	}

	public void addBuilder(Builder<T> b) {  
      // add an entry "b.getTypeTag() |−> b" to builders.   
      // ...  
      // add b.getInfo() to buildersInfo  
      // ...  
	}

	@Override  
	public T createInstance(JSONObject info) {  
		if (info == null) {  
			throw new IllegalArgumentException("’info’ cannot be null");  
		}

		// Look for a builder with a tag equals to info.getString("type"), in the
    //  map _builder, and call its createInstance method and return the result
    // if it is not null. The value you pass to createInstance is the following
    // because 'data' is optional:
    //
    //   info.has("data") ? info.getJSONObject("data") : new JSONObject()
    // …

    // If no builder is found or the result is null ...  
		throw new IllegalArgumentException("Unrecognized ‘info’:" + info.toString());  
	}

	@Override  
	public List<JSONObject> getInfo() {  
		return Collections.unmodifiableList(buildersInfo);  
	}  
}

```

### How to Create and Initialize Factories

We will use the `BuilderBasedFactory` class to create 3 factories (for strategies, for animals, and for regions). This example shows how we can create a strategy factory (it will be used in the [`Main`](#the-main-class) class):

```java
// initialize the strategies factory  
List<Builder<SelectionStrategy>> selectionStrategyBuilders = new ArrayList<>();  
selectionStrategyBuilders.add(new SelectFirstBuilder());  
selectionStrategyBuilders.add(new SelectClosestBuilder());  
Factory<SelectionStrategy> selectionStrategyFactory = new BuilderBasedFactory<SelectionStrategy>(selectionStrategyBuilders);

```

Remember that `SheepBuilder` and `WolfBuilder` require access to the strategy factory (must be passed as a parameter to their constructors).

## The `Main` Class

In the `simulator.launcher` package you can find an *incomplete version* of the `Main` class. This class processes command line arguments and starts the simulation. The class also analyzes some command line arguments using the `commons-cli` library (included in the lib directory). You will have to complete the `Main` class to analyze all possible arguments listed below.

Running `Main` with the `-h` (or `--help`) argument should show the following in the console (currently it does not show all parameters):

```
usage: simulator.launcher.Main [-dt <arg>] [-h] [-i <arg>] [-m <arg>] [-o  
       <arg>] [-sr <arg>] [-sv] [-t <arg>]  
 -dt,--delta-time <arg>      A double representing actual time, in  
                             seconds, per simulation step. Default value: 0.03.  
 -h,--help                   Print this message.  
 -i,--input <arg>            Initial configuration file.  
 -o,--output <arg>           Output file, where output is written.  
 -sv,--simple-viewer         Show the viewer window in console mode.  
 -t,--time <arg>             An real number representing the total  
                             simulation time in seconds. Default value: 10.0.

```

As an example of using the simulator using the command line, we show:

```
-i resources/examples/ex1.json -o resources/tmp/myout.jso -t 60.0 -dt 0.03 -sv

```

which executes the simulator using `resources/examples/ex1.json` as the input file and creating the output file `resources/tmp/myout.json`. The `-t` parameter indicates the simulation duration (`60.0` seconds), the `-dt` parameter indicates the time-per-step value in seconds (`0.03`), and `-sv` indicates that we want to show the viewer.

The provided code implements only the `-i` and `-t` options. You must extend it to implement the rest of the options as indicated in the help output above, and store their values in attributes to be able to use them from other methods.

Additionally, you must complete the part that executes the simulator:

* Complete the `initFactories` method to initialize the factories and store them in the corresponding attributes.
* Complete the `startBatchMode` method so that it does the following
	1. Load the input file into a `JSONObject`.
	2. Create the output file.
	3. Create an instance of `Simulator` passing the information it needs to its constructor.
	4. Create an instance of `Controller` passing the simulator to it.
	5. Call `loadData` passing the input `JSONObject`.
	6. Call the `run` method with the corresponding parameters.
	7. Close the output file.

The input file contains a `JSON` of the following form:

```json
{  
  "width": w,  
  "height": h,  
  "rows": c,  
  "cols": e,  
  "animals": [a0, a1, ..., ak],
  "regions": [r0, r1, ..., el]
}

```

Where `w`, `h`, `c` and `e` are integers (use them to create the `Simulator` instance), and `ai` and `ri` are JSON structures that `loadData` of the controller will use (see the section [The Controller](#the-controller)).

## Appendix

### Handling JSON in Java

[JavaScript Object Notation](https://www.json.org/) (JSON) is a standard file format that uses text and allows storing object properties using attribute-value pairs and arrays of data types. A JSON structure is text structured as follows:

```json
{ "key1": value1, ..., "valuen": valuen }

```

where `keyi` is a sequence of characters (representing a key) and `valuei` can be a number, a string, another JSON structure, or an array `[o1,...,ok]`, where `oi` can be like `valuei`.

In the lib directory there is a library that allows analyzing a `JSON` file and converting it into `Java` objects. This library can also be used to create `JSON` structures and convert them to strings. A usage example of this library is available in the `extra.json` package.

### The Object Viewer

The viewer is a class we provide to visualize the state of the simulator (in assignment 2 you will implement a more advanced graphical interface). The viewer class is `SimpleObjectViewer` which is located in `viewer.jar` (it is in the lib folder).

The class allows drawing a list of objects, where the description of each object is an instance of the record `SimpleObjectViewer.ObjInfo`

```java
public record ObjInfo(String tag, int x, int y, int size) {  
}

```

where `tag` is the object category, `(x,y)` is the coordinate where we want to draw the object, and size is the size (the length of a side of a square in pixels). Coordinates must be non-negative (if they are negative they do not appear in the window). The `x` coordinate is horizontal, the `y` coordinate is vertical, and the top-left corner is `(0,0)`. There is another constructor that does not receive `size` and uses `10` as a default value.

To draw animals it is necessary to convert a list of type `List<? extends AnimalInfo>` to `List<ObjInfo>` using the following method:

```java
private List<ObjInfo> toAnimalsInfo(List<? extends AnimalInfo> animals) {
	List<ObjInfo> ol = new ArrayList<>(animals.size());
	for (AnimalInfo a : animals)
		ol.add(new ObjInfo(a.getGeneticCode(), (int) a.getPosition().getX(), (int) a.getPosition().getY(),8));
	return ol;
}

```

You can replace the size `8` with something that depends on `a.getAge()` so that objects have sizes according to their age, for example `(int)Math.round(a.getAge())+2`. At the beginning of the run method in the `Controller` class, use the following code to create the instance of the `SimpleObjectViewer` class and draw the initial state:

```java
SimpleObjectViewer view = null;  
if (sv) {  
   MapInfo m = sim.getMapInfo();  
   view = new SimpleObjectViewer("[ECOSYSTEM]", m.getWidth(), m.getHeight(), m.getCols(), m.getRows());  
   view.update(toAnimalsInfo(sim.getAnimals()), sim.getTime(), dt);  
}

```

To draw the simulator state in each iteration, the following call can be used after the call to `sim.advance(dt)`:

```java
if (sv) view.update(toAnimalsInfo(sim.getAnimals()), sim.getTime(), dt);

```

Note that the `view.update` method pauses execution so that the real time passed since the last call to `view.update` is equal to (or greater than) `dt`, and in this way the real time and the simulator time will be similar (if you don't pass `dt` time does not pause and everything goes very fast).

At the end of the `run` method, the following code can be used to close the viewer window (it can also be closed with a click on the **x** icon like any other window):

```java
if (sv) view.close();

```

### How to Write to an `OutputStream`

Suppose `out` is a variable of type `OutputStream`. To write to it, it is convenient to use a `PrintStream` as follows:

```java
PrintStream p = new PrintStream(out);
//...

p.println(...);

```

### Input Examples

The `resources/examples` directory includes some examples of input files.

### Random Number Generation

In many parts of the simulator we use random numbers. In Java this is normally done using the `Random` class. This class uses a seed from which it generates random numbers, and by default uses the current time as a seed and thus in each execution we generate different random numbers. However, there are situations where *we want to generate the same random numbers in each execution*, for example when we debug the program, something that can be achieved if we always use the same `Random` instance with the same seed.

The `simulator.misc.Utils` class we provide includes an instance of the `Random` class as a static attribute `rand`. Always use it to guarantee that we generate random numbers using the same `Random` instance, and if you want to use the same seed in several executions you can add the following line to the beginning of the `Main.main` method (you can change the seed `2147483647l` to any number):

```java
Utils.RAND.setSeed(2147483647l);

```

### The `Vector2D` Class

A vector `a` is a point  in a 2D space, where  and  are real numbers (i.e., of type `double`). We can imagine a vector as a line going from  to the point .

In the `simulator.misc` package there is a `Vector2D` class, which implements a vector and offers corresponding operations:

| Operation | Description | In Vector2D |
| --- | --- | --- |
| **Sum** | $a+b$ is defined as the new vector $(a_1+b_1,a_2+b_2)$ | `a.plus(b)` |
| **Subtraction** |  $a-b$ is defined as the new vector $(a_1-b_1,a_2-b_2)$  | `a.minus(b)` |
| **Scalar multiplication** | $c \cdot a$, where $c$ is a real number, is defined as the new vector $(c\cdot a_1,c \cdot a_2)$ | `a.scale(c)` |
| **Length** | The length (or magnitude) of $a$ denoted as \|a\|$ is defined as $\sqrt{a_1^2+a_2^2}$ | `a.magnitude()` |
| **Direction** | The direction of a is a new vector going in the same direction as a but its length is 1, i.e., defined as $\frac{a}{\|a\|}$ | `a.direction()` |
| **Distance** | The distance between $a$ and $b$ is defined as $a-b$ | `a.distanceTo(b)` |

Nothing can be modified in this class. The `Vector2D` class is immutable, meaning it is not possible to change the state of an instance after creating it – operations (like plus, minus, etc.) return new instances.

### Adjusting positions

The following code can be used to adjust the position `(x,y)` so that it is within the map, assuming the map width is width and height is height:

```java
while (x >= width) x = (x - width);  
while (x < 0) x = (x + width);  
while (y >= height) y = (y - height);  
while (y < 0) y = (y + height);

```

This causes that when the animal exits one side, it appears on the other.

### Constants

It is highly recommended to use these constants instead of fixed values in the different formulas we mention in the other sections.

```java
    // used in Animal and subclasses
	final static double INIT_ENERGY = 100.0;
	final static double MUTATION_TOLERANCE = 0.2;
	final static double NEARBY_FACTOR = 60.0;
	final static double COLLISION_RANGE = 8;
	final static double HUNGER_DECAY_EXP_FACTOR = 0.007;
	final static double MAX_ENERGY = 100.0;
	final static double MAX_DESIRE = 100.0;

    // used in Sheep
	final static String SHEEP_GENETIC_CODE = "Sheep";
	final static double INIT_SIGHT_SHEEP = 40;
	final static double INIT_SPEED_SHEEP = 35;
	final static double BOOST_FACTOR_SHEEP = 2.0;
	final static double MAX_AGE_SHEEP = 8;
	final static double FOOD_DROP_BOOST_FACTOR_SHEEP = 1.2;
	final static double FOOD_DROP_RATE_SHEEP = 20.0;
	final static double DESIRE_THRESHOLD_SHEEP = 65.0;
	final static double DESIRE_INCREASE_RATE_SHEEP = 40.0;
	final static double PREGNANT_PROBABILITY_SHEEP = 0.9;

    // used in Wolf
	final static String WOLF_GENETIC_CODE = "Wolf";
	final static double INIT_SIGHT_WOLF = 50;
	final static double INIT_SPEED_WOLF = 60;
	final static double BOOST_FACTOR_WOLF = 3.0;
	final static double MAX_AGE_WOLF = 14.0;
	final static double FOOD_THRSHOLD_WOLF = 50.0;
	final static double FOOD_DROP_BOOST_FACTOR_WOLF = 1.2;
	final static double FOOD_DROP_RATE_WOLF = 18.0;
	final static double FOOD_DROP_DESIRE_WOLF = 10.0;
	final static double FOOD_EAT_VALUE_WOLF = 50.0;
	final static double DESIRE_THRESHOLD_WOLF = 65.0;
	final static double DESIRE_INCREASE_RATE_WOLF = 30.0;
	final static double PREGNANT_PROBABILITY_WOLF = 0.75;

	// used in DefaultRegion
	final static double FOOD_EAT_RATE_HERBS = 60.0;
	final static double FOOD_SHORTAGE_TH_HERBS = 5.0;
	final static double FOOD_SHORTAGE_EXP_HERBS = 2.0;

	// used in DynamicSupplyRegion
	final static double FOOD_EAT_RATE_HERBS = 60.0;
	final static double FOOD_SHORTAGE_TH_HERBS = 5.0;
	final static double FOOD_SHORTAGE_EXP_HERBS = 2.0;
	final static double INIT_FOOD = 100.0;
	final static double FACTOR = 2.0;

```
