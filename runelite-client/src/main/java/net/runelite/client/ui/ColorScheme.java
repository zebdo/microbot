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

/**
 * This class serves to hold commonly used UI colors.
 * Colors are dynamically determined based on the currently selected theme.
 */
public class ColorScheme
{
	private static Theme currentTheme = Theme.DEFAULT;

	/**
	 * Set the current theme for the color scheme
	 */
	public static void setTheme(Theme theme)
	{
		currentTheme = theme;
	}

	/**
	 * Get the current theme
	 */
	public static Theme getTheme()
	{
		return currentTheme;
	}

	/* The orange color used for the branding's accents */
	public static Color BRAND_ORANGE = currentTheme.getBrandColor();

	/* The orange color used for the branding's accents, with lowered opacity */
	public static Color BRAND_ORANGE_TRANSPARENT = currentTheme.getBrandColorTransparent();

	public static Color DARKER_GRAY_COLOR = currentTheme.getDarkerGrayColor();
	public static Color DARK_GRAY_COLOR = currentTheme.getDarkGrayColor();
	public static Color MEDIUM_GRAY_COLOR = currentTheme.getMediumGrayColor();
	public static Color LIGHT_GRAY_COLOR = currentTheme.getLightGrayColor();

	public static Color TEXT_COLOR = currentTheme.getTextColor();
	public static Color CONTROL_COLOR = currentTheme.getControlColor();
	public static Color BORDER_COLOR = currentTheme.getBorderColor();

	public static Color DARKER_GRAY_HOVER_COLOR = currentTheme.getDarkerGrayHoverColor();
	public static Color DARK_GRAY_HOVER_COLOR = currentTheme.getDarkGrayHoverColor();

	/* The color for the green progress bar (used in ge offers, farming tracker, etc)*/
	public static Color PROGRESS_COMPLETE_COLOR = currentTheme.getProgressCompleteColor();

	/* The color for the red progress bar (used in ge offers, farming tracker, etc)*/
	public static Color PROGRESS_ERROR_COLOR = currentTheme.getProgressErrorColor();

	/* The color for the orange progress bar (used in ge offers, farming tracker, etc)*/
	public static Color PROGRESS_INPROGRESS_COLOR = currentTheme.getProgressInProgressColor();

	/* The color for the price indicator in the ge search results */
	public static Color GRAND_EXCHANGE_PRICE = currentTheme.getGrandExchangePriceColor();

	/* The color for the high alch indicator in the ge search results */
	public static Color GRAND_EXCHANGE_ALCH = currentTheme.getGrandExchangeAlchColor();

	/* The color for the limit indicator in the ge search results */
	public static Color GRAND_EXCHANGE_LIMIT = currentTheme.getGrandExchangeLimitColor();

	/* The background color of the scrollbar's track */
	public static Color SCROLL_TRACK_COLOR = currentTheme.getScrollTrackColor();

	/**
	 * Update all color fields based on the current theme
	 * This should be called after setTheme() to apply the theme changes
	 */
	public static void updateColors()
	{
		BRAND_ORANGE = currentTheme.getBrandColor();
		BRAND_ORANGE_TRANSPARENT = currentTheme.getBrandColorTransparent();
		DARKER_GRAY_COLOR = currentTheme.getDarkerGrayColor();
		DARK_GRAY_COLOR = currentTheme.getDarkGrayColor();
		MEDIUM_GRAY_COLOR = currentTheme.getMediumGrayColor();
		LIGHT_GRAY_COLOR = currentTheme.getLightGrayColor();
		TEXT_COLOR = currentTheme.getTextColor();
		CONTROL_COLOR = currentTheme.getControlColor();
		BORDER_COLOR = currentTheme.getBorderColor();
		DARKER_GRAY_HOVER_COLOR = currentTheme.getDarkerGrayHoverColor();
		DARK_GRAY_HOVER_COLOR = currentTheme.getDarkGrayHoverColor();
		PROGRESS_COMPLETE_COLOR = currentTheme.getProgressCompleteColor();
		PROGRESS_ERROR_COLOR = currentTheme.getProgressErrorColor();
		PROGRESS_INPROGRESS_COLOR = currentTheme.getProgressInProgressColor();
		GRAND_EXCHANGE_PRICE = currentTheme.getGrandExchangePriceColor();
		GRAND_EXCHANGE_ALCH = currentTheme.getGrandExchangeAlchColor();
		GRAND_EXCHANGE_LIMIT = currentTheme.getGrandExchangeLimitColor();
		SCROLL_TRACK_COLOR = currentTheme.getScrollTrackColor();
	}
}