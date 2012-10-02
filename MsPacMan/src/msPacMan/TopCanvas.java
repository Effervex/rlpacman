/*
 *    This file is part of the CERRLA algorithm, but was originally obtained
 *    from bennychow.com. Used with permission.
 *
 *    CERRLA is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    CERRLA is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with CERRLA. If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    src/msPacMan/TopCanvas.java
 *    Copyright (C) 2012 Samuel Sarjant
 */
package msPacMan;

import java.awt.*;


// Top Right Canvas which is repainted many times because
// it contains the Score string.
public class TopCanvas extends Canvas {
	Font m_font;
	GameModel m_gameModel;

	Image m_offImage;
	Graphics m_offGraphics;
	Dimension m_offDim;

	public TopCanvas(GameModel gameModel, int width, int height) {
		super();
		setSize(width, height);
		m_gameModel = gameModel;
		m_font = new Font("Helvetica", Font.BOLD, 14);
	}

	@Override
	public void update(Graphics g) {
		paint(g);
	}

	@Override
	public void paint(Graphics g) {
		int y;
		int x;
		Dimension dim = getSize();

		// Create double buffer if it does not exist or is not
		// the right size
		if (m_offImage == null || m_offDim.width != dim.width
				|| m_offDim.height != dim.height) {
			m_offDim = dim;
			m_offImage = createImage(m_offDim.width, m_offDim.height);
			m_offGraphics = m_offImage.getGraphics();
		}

		m_offGraphics.setColor(Color.black);
		m_offGraphics.fillRect(0, 0, m_offDim.width, m_offDim.height);

		m_offGraphics.setColor(Color.white);

		m_offGraphics.setFont(m_font);
		FontMetrics fm = m_offGraphics.getFontMetrics();

		// HIGH SCORE
		y = 20 + fm.getAscent() + fm.getDescent();
		x = 0;
		m_offGraphics.drawString("HIGH SCORE", x, y);

		y += fm.getAscent() + fm.getDescent();
		x = fm.stringWidth("HIGH SCORE")
				- fm.stringWidth(Integer.toString(m_gameModel.m_highScore));
		m_offGraphics.drawString(Integer.toString(m_gameModel.m_highScore), x,
				y);

		// SCORE
		y += 10 + fm.getAscent() + fm.getDescent();
		x = fm.stringWidth("HIGH SCORE") - fm.stringWidth("SCORE");
		m_offGraphics.drawString("SCORE", x, y);

		y += fm.getAscent() + fm.getDescent();
		x = fm.stringWidth("HIGH SCORE")
				- fm
						.stringWidth(Integer
								.toString(m_gameModel.m_player.m_score));
		m_offGraphics.drawString(
				Integer.toString(m_gameModel.m_player.m_score), x, y);

		g.drawImage(m_offImage, 0, 0, this);
	}
}