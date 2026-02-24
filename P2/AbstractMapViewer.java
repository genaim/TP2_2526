package simulator.view;

import java.util.List;

import javax.swing.JComponent;

import simulator.model.AnimalInfo;
import simulator.model.MapInfo;

@SuppressWarnings("serial")
public abstract class AbstractMapViewer extends JComponent {

	public abstract void update(List<AnimalInfo> objs, Double time);

	public abstract void reset(double time, MapInfo map, List<AnimalInfo> animals);
}
