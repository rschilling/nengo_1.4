package ca.neo.ui.models.icons;

import ca.neo.ui.models.nodes.UINodeContainer;
import ca.neo.ui.style.Style;
import ca.shu.ui.lib.objects.GText;
import edu.umd.cs.piccolo.PNode;

/**
 * Icon for a Node Container. The size of this icon scales depending on the
 * number of nodes contained by the model.
 * 
 * @author Shu
 * 
 */
public abstract class NodeContainerIcon extends ModelIcon {

	private static final long serialVersionUID = 1L;

	public static final float MAX_SCALE = 1.5f;

	public static final float MIN_SCALE = 0.5f;

	private int myNumOfNodes = -1;

	private final GText sizeLabel;

	public NodeContainerIcon(UINodeContainer parent, PNode icon) {
		super(parent, icon);
		sizeLabel = new GText("");
		sizeLabel.setFont(Style.FONT_SMALL);
		sizeLabel.setConstrainWidthToTextWidth(true);
		addChild(sizeLabel);
		layoutChildren();
		modelUpdated();
	}

	/**
	 * Scales the icon display size depending on how many nodes are contained
	 * within it
	 * 
	 */
	private void updateIconScale() {

		int numOfNodes = getModelParent().getNodesCount();

		if (myNumOfNodes == numOfNodes) {
			return;
		}
		myNumOfNodes = numOfNodes;

		sizeLabel.setText(myNumOfNodes + " nodes");

		float numOfNodesNormalized;
		if (numOfNodes >= getNodeCountNormalization())
			numOfNodesNormalized = 1;
		else {
			numOfNodesNormalized = (float) Math.sqrt((float) numOfNodes
					/ (float) getNodeCountNormalization());
		}

		float scale = MIN_SCALE
				+ (numOfNodesNormalized * (MAX_SCALE - MIN_SCALE));

		getIconReal().setScale(scale);

	}

	protected abstract int getNodeCountNormalization();

	@Override
	protected void layoutChildren() {
		super.layoutChildren();

		sizeLabel.setOffset(0, -(sizeLabel.getHeight() + 1));

		sizeLabel.moveToFront();
	}

	@Override
	protected void modelUpdated() {
		super.modelUpdated();
		updateIconScale();
	}

	@Override
	public UINodeContainer getModelParent() {

		return (UINodeContainer) super.getModelParent();
	}

}