package net.runelite.client.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Simple configuration action button that appears inside plugin configuration panels.
 */
@RequiredArgsConstructor
public class ConfigButton
{
        @Getter
        private final String label;

        private final Runnable onClick;

        /**
         * Invoke the configured action if one was supplied.
         */
        public void press()
        {
                if (onClick != null)
                {
                        onClick.run();
                }
        }
}
