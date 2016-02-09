~(load "macros.scm")

package brya3525;

~(java-import '(
			(java
				(awt
					Color
					Font
					Graphics2D))
			(spacesettlers
				(utilities
				 	Position)
				(graphics
				 	SpacewarGraphics))))

/**
 * Draw some text
 * * @author russell
 *
 */
public class TextGraphics extends SpacewarGraphics {
	String text;
	Color textColour;
	Position textPosition;

	public TextGraphics(String text, Position textPosition, Color textColour) {
		super(1,1);
		this.text = text;
		this.textColour = textColour;
		this.textPosition = textPosition;
	}

	public Position getActualLocation() {
		return textPosition;
	}

	@Override
	public void draw(Graphics2D graphics) {
		final Font font = new Font("Arial", Font.BOLD, 12);
		graphics.setFont(font);

		String text = this.text;
		graphics.setPaint(this.textColour);
		graphics.drawString(text, (int) this.textPosition.getX(), (int) this.textPosition.getY());
		
	}

	@Override
	public boolean isDrawable() {
		return true;
	}

}
