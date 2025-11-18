/*
 * Copyright (c) 2018, Psikoi <https://github.com/psikoi>
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

import java.awt.Color;
import lombok.Getter;

/**
 * Available UI themes for the client
 */
@Getter
public enum Theme
{
	DEFAULT("Default"),
	CHRISTMAS("Christmas");

	private final String name;

	Theme(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}

	/**
	 * Get brand accent color for this theme
	 */
	public Color getBrandColor()
	{
		switch (this)
		{
			case CHRISTMAS:
				return new Color(200, 40, 40); // Christmas red
			case DEFAULT:
			default:
				return new Color(220, 138, 0); // RuneLite orange
		}
	}

	/**
	 * Get brand accent color with transparency for this theme
	 */
	public Color getBrandColorTransparent()
	{
		switch (this)
		{
			case CHRISTMAS:
				return new Color(200, 40, 40, 120); // Christmas red transparent
			case DEFAULT:
			default:
				return new Color(220, 138, 0, 120); // RuneLite orange transparent
		}
	}

	/**
	 * Get darker gray color for this theme
	 */
	public Color getDarkerGrayColor()
	{
		switch (this)
		{
			case CHRISTMAS:
				return new Color(25, 35, 25); // Dark green tint
			case DEFAULT:
			default:
				return new Color(30, 30, 30);
		}
	}

	/**
	 * Get dark gray color for this theme
	 */
	public Color getDarkGrayColor()
	{
		switch (this)
		{
			case CHRISTMAS:
				return new Color(35, 45, 35); // Slightly lighter dark green tint
			case DEFAULT:
			default:
				return new Color(40, 40, 40);
		}
	}

	/**
	 * Get medium gray color for this theme
	 */
	public Color getMediumGrayColor()
	{
		switch (this)
		{
			case CHRISTMAS:
				return new Color(70, 85, 70); // Medium green tint
			case DEFAULT:
			default:
				return new Color(77, 77, 77);
		}
	}

	/**
	 * Get light gray color for this theme
	 */
	public Color getLightGrayColor()
	{
		switch (this)
		{
			case CHRISTMAS:
				return new Color(200, 215, 200); // Light green tint
			case DEFAULT:
			default:
				return new Color(165, 165, 165);
		}
	}

	/**
	 * Get text color for this theme
	 */
	public Color getTextColor()
	{
		switch (this)
		{
			case CHRISTMAS:
				return new Color(220, 235, 220); // Slightly greenish white
			case DEFAULT:
			default:
				return new Color(198, 198, 198);
		}
	}

	/**
	 * Get control color for this theme
	 */
	public Color getControlColor()
	{
		switch (this)
		{
			case CHRISTMAS:
				return new Color(25, 35, 25); // Dark green tint
			case DEFAULT:
			default:
				return new Color(30, 30, 30);
		}
	}

	/**
	 * Get border color for this theme
	 */
	public Color getBorderColor()
	{
		switch (this)
		{
			case CHRISTMAS:
				return new Color(180, 140, 0); // Gold
			case DEFAULT:
			default:
				return new Color(23, 23, 23);
		}
	}

	/**
	 * Get darker gray hover color for this theme
	 */
	public Color getDarkerGrayHoverColor()
	{
		switch (this)
		{
			case CHRISTMAS:
				return new Color(55, 70, 55); // Hover green tint
			case DEFAULT:
			default:
				return new Color(60, 60, 60);
		}
	}

	/**
	 * Get dark gray hover color for this theme
	 */
	public Color getDarkGrayHoverColor()
	{
		switch (this)
		{
			case CHRISTMAS:
				return new Color(30, 40, 30); // Hover green tint
			case DEFAULT:
			default:
				return new Color(35, 35, 35);
		}
	}

	/**
	 * Get progress complete color for this theme
	 */
	public Color getProgressCompleteColor()
	{
		switch (this)
		{
			case CHRISTMAS:
				return new Color(40, 180, 40); // Bright Christmas green
			case DEFAULT:
			default:
				return new Color(55, 240, 70);
		}
	}

	/**
	 * Get progress error color for this theme
	 */
	public Color getProgressErrorColor()
	{
		switch (this)
		{
			case CHRISTMAS:
				return new Color(200, 40, 40); // Christmas red
			case DEFAULT:
			default:
				return new Color(230, 30, 30);
		}
	}

	/**
	 * Get progress in-progress color for this theme
	 */
	public Color getProgressInProgressColor()
	{
		switch (this)
		{
			case CHRISTMAS:
				return new Color(200, 150, 30); // Gold
			case DEFAULT:
			default:
				return new Color(230, 150, 30);
		}
	}

	/**
	 * Get Grand Exchange price color for this theme
	 */
	public Color getGrandExchangePriceColor()
	{
		switch (this)
		{
			case CHRISTMAS:
				return new Color(100, 220, 100); // Bright green
			case DEFAULT:
			default:
				return new Color(110, 225, 110);
		}
	}

	/**
	 * Get Grand Exchange alch color for this theme
	 */
	public Color getGrandExchangeAlchColor()
	{
		switch (this)
		{
			case CHRISTMAS:
				return new Color(220, 180, 100); // Gold
			case DEFAULT:
			default:
				return new Color(240, 207, 123);
		}
	}

	/**
	 * Get Grand Exchange limit color for this theme
	 */
	public Color getGrandExchangeLimitColor()
	{
		// Keep same for all themes
		return new Color(50, 160, 250);
	}

	/**
	 * Get scroll track color for this theme
	 */
	public Color getScrollTrackColor()
	{
		switch (this)
		{
			case CHRISTMAS:
				return new Color(20, 30, 20); // Very dark green tint
			case DEFAULT:
			default:
				return new Color(25, 25, 25);
		}
	}
}
