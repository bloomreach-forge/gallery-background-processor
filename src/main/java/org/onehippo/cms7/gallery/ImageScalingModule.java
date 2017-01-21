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

package org.onehippo.cms7.gallery;

import org.apache.wicket.util.io.IOUtils;
import org.hippoecm.frontend.plugins.gallery.imageutil.ImageUtils;
import org.hippoecm.frontend.plugins.gallery.imageutil.ScalingParameters;
import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.processor.ScalingGalleryProcessor;
import org.hippoecm.frontend.plugins.gallery.processor.ScalingGalleryProcessorPlugin;
import org.hippoecm.frontend.plugins.gallery.processor.event.ImageVariantEvent;
import org.hippoecm.repository.gallery.HippoGalleryNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.cms7.services.eventbus.Subscribe;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.*;

import static org.hippoecm.frontend.plugins.gallery.processor.ScalingGalleryProcessorPlugin.*;

public class ImageScalingModule extends AbstractReconfigurableDaemonModule {

    private static final Logger log = LoggerFactory.getLogger(ImageScalingModule.class);

    private static final long MAX_DELAY = Long.MAX_VALUE;
    private static final int DEFAULT_MAX_RETRY = 5;
    private static final int DEFAULT_DELAY = 1000;
    private Session session;
    private ScalingGalleryProcessor scalingProcessor;
    private int maxRetry = DEFAULT_MAX_RETRY;
    private int minDelay = DEFAULT_DELAY;

    @Subscribe
    public void handleEvent(ImageVariantEvent event) {
        log.debug("Processing variant: {}", event.variant());

        try {
            retry(() -> {
                InputStream stream = null;
                try {
                    final Node root = session.getNode(event.node());
                    final Node original = root.getNode(HippoGalleryNodeType.IMAGE_SET_ORIGINAL);
                    final Node variantNode = root.addNode(event.variant(), event.type());
                    stream = original.getProperty(JcrConstants.JCR_DATA).getBinary().getStream();
                    if (scalingProcessor == null) {
                        scalingProcessor = createScalingGalleryProcessor();
                    }
                    scalingProcessor.initGalleryResource(variantNode, stream, event.mimeType(), event.fileName(), Calendar.getInstance());
                    session.save();

                } catch (final PathNotFoundException e) {
                    log.debug("Image variant not found, will retry again");
                    throw e;
                } catch (RepositoryException e) {
                    log.error("", e);
                    refresh();
                    throw e;
                } finally {
                    IOUtils.closeQuietly(stream);
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Error creating variant", e);
        }

    }

    public void retry(final Callable<Void> command) throws Exception {
        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        try {
            long delay = minDelay;
            for (int i = 0; i < maxRetry; i++) {
                final ScheduledFuture<Void> future = executor.schedule(command, delay, TimeUnit.MILLISECONDS);
                try {
                    future.get();
                    return;
                } catch (final ExecutionException e) {
                    Throwable cause = e.getCause();
                    /*
                     * In case of PathNotFoundException (original might not be there yet)
                     * we want to retry to create image at later time
                     */
                    final boolean lastRetry = i == (maxRetry - 1);
                    if (lastRetry || !(cause instanceof PathNotFoundException)) {
                        executor.shutdownNow();
                        return;
                    }
                    delay = backoff(i);

                    log.info("retry #{} scheduled in {} milliseconds due to {}", i + 1, delay, cause.getMessage());
                }

            }
        } finally {
            if (!executor.isShutdown()) {
                executor.shutdownNow();
            }

        }
    }

    /**
     * Exponential backoff
     *
     * @param attempt current attempt
     * @return exponential backoff in milliseconds
     */
    public long backoff(int attempt) {
        long duration = minDelay * (long) Math.pow(2, attempt);
        if (duration < 0) {
            duration = MAX_DELAY;
        }
        return Math.min(Math.max(duration, minDelay), MAX_DELAY);
    }


    private void refresh() {
        try {
            session.refresh(false);
        } catch (RepositoryException e) {
            log.error("", e);
        }
    }

    @Override
    protected void doConfigure(final Node node) throws RepositoryException {
        log.debug("Reconfiguring BackgroundScalingModule: {}");
        maxRetry = getAsInteger(node, "maxRetry", DEFAULT_MAX_RETRY);
        minDelay = getAsInteger(node, "delay", DEFAULT_DELAY);
    }

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        this.session = session;
        HippoServiceRegistry.registerService(this, HippoEventBus.class);

    }

    @Override
    protected void doShutdown() {
        HippoServiceRegistry.unregisterService(this, HippoEventBus.class);
    }

    protected ScalingGalleryProcessor createScalingGalleryProcessor() throws RepositoryException {
        final Node config = session.getNode("/hippo:configuration/hippo:frontend/cms/cms-services/galleryProcessorService");
        final ScalingGalleryProcessor processor = new ScalingGalleryProcessor();
        final boolean backgroundProcessing = JcrUtils.getBooleanProperty(config, GalleryProcessor.GALLERY_BACKGROUND_PROCESSING, false);
        processor.setBackgroundProcessing(backgroundProcessing);
        log.debug("Using background processing: {}", backgroundProcessing);
        final NodeIterator nodes = config.getNodes();
        while (nodes.hasNext()) {
            final Node scaleConfig = nodes.nextNode();

            final String nodeName = scaleConfig.getName();
            final int width = getAsInteger(scaleConfig, ScalingGalleryProcessorPlugin.CONFIG_PARAM_WIDTH, DEFAULT_WIDTH);
            final int height = getAsInteger(scaleConfig, CONFIG_PARAM_HEIGHT, DEFAULT_HEIGHT);
            final boolean upscaling = JcrUtils.getBooleanProperty(scaleConfig, CONFIG_PARAM_UPSCALING, DEFAULT_UPSCALING);
            final float compressionQuality = (float) getAsDouble(scaleConfig, CONFIG_PARAM_COMPRESSION, DEFAULT_COMPRESSION);
            final String strategyName = JcrUtils.getStringProperty(scaleConfig, CONFIG_PARAM_OPTIMIZE, DEFAULT_OPTIMIZE);
            final Map<String, ImageUtils.ScalingStrategy> SCALING_STRATEGY_MAP = getScalingStrategyMap();
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

        return processor;
    }

    private double getAsDouble(final Node node, final String property, final double defaultValue) throws RepositoryException {
        if (node.hasProperty(property)) {

            return node.getProperty(property).getDouble();
        }
        return defaultValue;
    }

    private int getAsInteger(final Node node, final String property, final int defaultValue) throws RepositoryException {
        if (node.hasProperty(property)) {

            return (int) node.getProperty(property).getLong();
        }
        return defaultValue;
    }

}
