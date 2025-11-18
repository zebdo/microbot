/*
 * Copyright (c) 2024, Microbot Contributors
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.ui;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Utility class for painting Christmas-themed decorations on UI components
 */
public class ChristmasDecorator
{
	private static final Random RANDOM = new Random();
	private static final Color SNOW_COLOR = new Color(255, 255, 255, 180);
	private static final Color TREE_GREEN = new Color(34, 139, 34);
	private static final Color TREE_DARK_GREEN = new Color(25, 100, 25);
	private static final Color TREE_TRUNK = new Color(139, 69, 19);
	private static final Color STAR_GOLD = new Color(255, 215, 0);
	private static final Color ORNAMENT_RED = new Color(220, 20, 60);
	private static final Color ORNAMENT_GOLD = new Color(218, 165, 32);

	/**
	 * Paint snowflakes on a component
	 *
	 * @param g graphics context
	 * @param width component width
	 * @param height component height
	 * @param count number of snowflakes to paint
	 */
	public static void paintSnowflakes(Graphics2D g, int width, int height, int count)
	{
		if (ColorScheme.getTheme() != Theme.CHRISTMAS)
		{
			return;
		}

		Graphics2D g2d = (Graphics2D) g.create();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Use component dimensions to seed consistent snowflake positions
		Random snowRandom = new Random(width * 31L + height * 17L);

		for (int i = 0; i < count; i++)
		{
			int x = snowRandom.nextInt(width);
			int y = snowRandom.nextInt(height);
			int size = 2 + snowRandom.nextInt(3);

			// Draw snowflake
			g2d.setColor(new Color(255, 255, 255, 100 + snowRandom.nextInt(100)));

			// Simple snowflake shape (6-pointed star)
			for (int angle = 0; angle < 360; angle += 60)
			{
				double rad = Math.toRadians(angle);
				int x2 = x + (int) (Math.cos(rad) * size);
				int y2 = y + (int) (Math.sin(rad) * size);
				g2d.drawLine(x, y, x2, y2);
			}

			// Center dot
			g2d.fillOval(x - 1, y - 1, 2, 2);
		}

		g2d.dispose();
	}

	/**
	 * Paint a small Christmas tree icon
	 *
	 * @param g graphics context
	 * @param x x position
	 * @param y y position
	 * @param size size of the tree
	 */
	public static void paintChristmasTree(Graphics2D g, int x, int y, int size)
	{
		if (ColorScheme.getTheme() != Theme.CHRISTMAS)
		{
			return;
		}

		Graphics2D g2d = (Graphics2D) g.create();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int treeHeight = size;
		int treeWidth = (int) (size * 0.8);
		int trunkHeight = size / 5;
		int trunkWidth = size / 6;

		// Draw tree (three triangular layers)
		for (int layer = 0; layer < 3; layer++)
		{
			Path2D.Double triangle = new Path2D.Double();
			int layerY = y + (layer * treeHeight / 4);
			int layerWidth = treeWidth - (layer * treeWidth / 6);

			triangle.moveTo(x, layerY);
			triangle.lineTo(x - layerWidth / 2, layerY + treeHeight / 3);
			triangle.lineTo(x + layerWidth / 2, layerY + treeHeight / 3);
			triangle.closePath();

			g2d.setColor(layer % 2 == 0 ? TREE_GREEN : TREE_DARK_GREEN);
			g2d.fill(triangle);
		}

		// Draw trunk
		g2d.setColor(TREE_TRUNK);
		g2d.fillRect(x - trunkWidth / 2, y + treeHeight, trunkWidth, trunkHeight);

		// Draw star on top
		g2d.setColor(STAR_GOLD);
		paintStar(g2d, x, y - size / 6, size / 4);

		// Draw ornaments
		Random ornamentRandom = new Random(x * 31L + y * 17L);
		for (int i = 0; i < 3; i++)
		{
			int ornamentX = x + (ornamentRandom.nextInt(treeWidth) - treeWidth / 2);
			int ornamentY = y + ornamentRandom.nextInt(treeHeight);
			Color ornamentColor = i % 2 == 0 ? ORNAMENT_RED : ORNAMENT_GOLD;
			g2d.setColor(ornamentColor);
			g2d.fillOval(ornamentX - 2, ornamentY - 2, 4, 4);
		}

		g2d.dispose();
	}

	/**
	 * Paint a star shape
	 *
	 * @param g graphics context
	 * @param centerX center x position
	 * @param centerY center y position
	 * @param size size of the star
	 */
	private static void paintStar(Graphics2D g, int centerX, int centerY, int size)
	{
		Path2D.Double star = new Path2D.Double();

		for (int i = 0; i < 10; i++)
		{
			double angle = Math.toRadians(i * 36 - 90);
			double radius = (i % 2 == 0) ? size : size / 2.5;
			double x = centerX + Math.cos(angle) * radius;
			double y = centerY + Math.sin(angle) * radius;

			if (i == 0)
			{
				star.moveTo(x, y);
			}
			else
			{
				star.lineTo(x, y);
			}
		}
		star.closePath();

		g.fill(star);
	}

	/**
	 * Paint Christmas lights along a border
	 *
	 * @param g graphics context
	 * @param width component width
	 * @param height component height
	 */
	public static void paintChristmasLights(Graphics2D g, int width, int height)
	{
		if (ColorScheme.getTheme() != Theme.CHRISTMAS)
		{
			return;
		}

		Graphics2D g2d = (Graphics2D) g.create();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Color[] lightColors = {
			new Color(255, 50, 50),   // Red
			new Color(50, 255, 50),   // Green
			new Color(50, 50, 255),   // Blue
			new Color(255, 215, 0),   // Gold
			new Color(255, 255, 255)  // White
		};

		int spacing = 20;
		int lightSize = 6;

		// Top border lights
		for (int x = spacing / 2; x < width; x += spacing)
		{
			Color lightColor = lightColors[(x / spacing) % lightColors.length];
			g2d.setColor(lightColor);
			g2d.fillOval(x - lightSize / 2, 2, lightSize, lightSize);

			// Glow effect
			g2d.setColor(new Color(lightColor.getRed(), lightColor.getGreen(), lightColor.getBlue(), 50));
			g2d.fillOval(x - lightSize, 0, lightSize * 2, lightSize * 2);
		}

		g2d.dispose();
	}

	/**
	 * Paint festive snow accumulation on top of a component
	 *
	 * @param g graphics context
	 * @param width component width
	 */
	public static void paintSnowAccumulation(Graphics2D g, int width)
	{
		if (ColorScheme.getTheme() != Theme.CHRISTMAS)
		{
			return;
		}

		Graphics2D g2d = (Graphics2D) g.create();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Draw wavy snow accumulation on top edge
		Path2D.Double snowPath = new Path2D.Double();
		snowPath.moveTo(0, 5);

		for (int x = 0; x <= width; x += 10)
		{
			int y = 3 + (int) (Math.sin(x / 10.0) * 2);
			snowPath.lineTo(x, y);
		}

		snowPath.lineTo(width, 0);
		snowPath.lineTo(0, 0);
		snowPath.closePath();

		g2d.setColor(new Color(255, 255, 255, 180));
		g2d.fill(snowPath);

		g2d.dispose();
	}

	/**
	 * Create a Christmas tree checkbox icon
	 *
	 * @param size icon size
	 * @param selected whether the checkbox is selected
	 * @return BufferedImage of the Christmas tree checkbox icon
	 */
	public static BufferedImage createChristmasCheckboxIcon(int size, boolean selected)
	{
		BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = icon.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Draw background
		if (selected)
		{
			g.setColor(new Color(34, 139, 34, 100));
			g.fillRoundRect(0, 0, size, size, 4, 4);
		}

		// Draw border
		g.setColor(ColorScheme.BORDER_COLOR);
		g.drawRoundRect(0, 0, size - 1, size - 1, 4, 4);

		// Draw Christmas tree if selected
		if (selected)
		{
			paintChristmasTree(g, size / 2, size / 4, size / 2);
		}

		g.dispose();
		return icon;
	}

	/**
	 * Paint festive holly decoration
	 *
	 * @param g graphics context
	 * @param x x position
	 * @param y y position
	 * @param size size of holly decoration
	 */
	public static void paintHolly(Graphics2D g, int x, int y, int size)
	{
		if (ColorScheme.getTheme() != Theme.CHRISTMAS)
		{
			return;
		}

		Graphics2D g2d = (Graphics2D) g.create();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Draw holly leaves
		g2d.setColor(new Color(34, 139, 34));

		// Left leaf
		Path2D.Double leftLeaf = new Path2D.Double();
		leftLeaf.moveTo(x, y);
		leftLeaf.curveTo(x - size / 2, y - size / 3, x - size / 2, y + size / 3, x, y + size / 2);
		leftLeaf.closePath();
		g2d.fill(leftLeaf);

		// Right leaf
		Path2D.Double rightLeaf = new Path2D.Double();
		rightLeaf.moveTo(x, y);
		rightLeaf.curveTo(x + size / 2, y - size / 3, x + size / 2, y + size / 3, x, y + size / 2);
		rightLeaf.closePath();
		g2d.fill(rightLeaf);

		// Draw berries
		g2d.setColor(ORNAMENT_RED);
		g2d.fillOval(x - 3, y + size / 3, 6, 6);
		g2d.fillOval(x - 6, y + size / 4, 5, 5);
		g2d.fillOval(x + 3, y + size / 4, 5, 5);

		g2d.dispose();
	}
}
