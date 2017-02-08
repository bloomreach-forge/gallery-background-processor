/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.labs.gallery;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.imageutil.ImageUtils;
import org.hippoecm.frontend.plugins.gallery.imageutil.ScalingParameters;
import org.hippoecm.frontend.plugins.gallery.processor.ScalingGalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.processor.ScalingGalleryProcessorPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ScalingGalleryProcessorPlugin  that creates a BackgroundScalingGalleryProcessor
 */
public class BackgroundScalingGalleryProcessorPlugin extends ScalingGalleryProcessorPlugin {

    private static final Logger log = LoggerFactory.getLogger(BackgroundScalingGalleryProcessorPlugin.class);

    static final String CONFIG_PARAM_WIDTH = "width";
    static final String CONFIG_PARAM_HEIGHT = "height";
    static final String CONFIG_PARAM_UPSCALING = "upscaling";
    static final String CONFIG_PARAM_OPTIMIZE = "optimize";
    static final String CONFIG_PARAM_COMPRESSION = "compression";

    static final int DEFAULT_WIDTH = 0;
    static final int DEFAULT_HEIGHT = 0;
    static final boolean DEFAULT_UPSCALING = false;
    static final String DEFAULT_OPTIMIZE = "quality";
    static final double DEFAULT_COMPRESSION = 1.0;

    private static final Map<String, ImageUtils.ScalingStrategy> SCALING_STRATEGY_MAP = new LinkedHashMap<>();

    static {
        SCALING_STRATEGY_MAP.put("auto", ImageUtils.ScalingStrategy.AUTO);
        SCALING_STRATEGY_MAP.put("speed", ImageUtils.ScalingStrategy.SPEED);
        SCALING_STRATEGY_MAP.put("speed.and.quality", ImageUtils.ScalingStrategy.SPEED_AND_QUALITY);
        SCALING_STRATEGY_MAP.put("quality", ImageUtils.ScalingStrategy.QUALITY);
        SCALING_STRATEGY_MAP.put("best.quality", ImageUtils.ScalingStrategy.BEST_QUALITY);
    }

    public BackgroundScalingGalleryProcessorPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected ScalingGalleryProcessor createScalingGalleryProcessor(IPluginConfig config) {
        final ScalingGalleryProcessor processor = new BackgroundScalingGalleryProcessor();

        for (IPluginConfig scaleConfig : config.getPluginConfigSet()) {
            final String nodeName = StringUtils.substringAfterLast(scaleConfig.getName(), ".");

            if (!StringUtils.isEmpty(nodeName)) {
                final int width = scaleConfig.getAsInteger(CONFIG_PARAM_WIDTH, DEFAULT_WIDTH);
                final int height = scaleConfig.getAsInteger(CONFIG_PARAM_HEIGHT, DEFAULT_HEIGHT);
                final boolean upscaling = scaleConfig.getAsBoolean(CONFIG_PARAM_UPSCALING, DEFAULT_UPSCALING);
                final float compressionQuality = (float) scaleConfig.getAsDouble(CONFIG_PARAM_COMPRESSION, DEFAULT_COMPRESSION);

                final String strategyName = scaleConfig.getString(CONFIG_PARAM_OPTIMIZE, DEFAULT_OPTIMIZE);
                ImageUtils.ScalingStrategy strategy = SCALING_STRATEGY_MAP.get(strategyName);
                if (strategy == null) {
                    log.warn("Image variant '{}' specifies an unknown scaling optimization strategy '{}'. Possible values are {}. Falling back to '{}' instead.",
                            nodeName, strategyName, SCALING_STRATEGY_MAP.keySet(), DEFAULT_OPTIMIZE);
                    strategy = SCALING_STRATEGY_MAP.get(DEFAULT_OPTIMIZE);
                }

                final ScalingParameters parameters = new ScalingParameters(width, height, upscaling, strategy, compressionQuality);
                log.debug("Scaling parameters for {}: {}", nodeName, parameters);
                processor.addScalingParameters(nodeName, parameters);
            }
        }

        return processor;
    }

    static Map<String, ImageUtils.ScalingStrategy> getScalingStrategyMap() {
        return SCALING_STRATEGY_MAP;
    }
}
