/*
 * Copyright 2017-2019 BloomReach Inc (https://www.bloomreach.com)
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

package org.bloomreach.forge.gallery;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.util.io.IOUtils;

import org.hippoecm.frontend.plugins.gallery.imageutil.ImageUtils;
import org.hippoecm.frontend.plugins.gallery.imageutil.ScalingParameters;
import org.hippoecm.frontend.plugins.gallery.processor.ScalingGalleryProcessor;
import org.hippoecm.repository.gallery.HippoGalleryNodeType;
import org.hippoecm.repository.util.JcrUtils;

import org.onehippo.cms7.services.eventbus.HippoEventListenerRegistry;
import org.onehippo.cms7.services.eventbus.Subscribe;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.onehippo.repository.util.JcrConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Daemon module that listens for image creation events and creates images variants from it.
 */
public class BackgroundGalleryProcessorModule extends AbstractReconfigurableDaemonModule {

    private static final Logger log = LoggerFactory.getLogger(BackgroundGalleryProcessorModule.class);

    private static final String GALLERY_PROCESSOR_SERVICE_PATH = "/hippo:configuration/hippo:frontend/cms/cms-services/galleryProcessorService";

    private static final long MAX_DELAY = Long.MAX_VALUE;
    private static final int DEFAULT_MAX_RETRY = 5;
    private static final int DEFAULT_DELAY = 1000;

    private ScalingGalleryProcessor scalingProcessor;
    private int maxRetry = DEFAULT_MAX_RETRY;
    private int delay = DEFAULT_DELAY;

    @Subscribe
    public void handleEvent(ImageCreationEvent event) {
        log.debug("Received ImageCreationEvent for nodePath {} with variants: {}", event.nodePath(), event.variants());

        try {
            retry(() -> {
                InputStream stream = null;
                try {
                    if (scalingProcessor == null) {
                        scalingProcessor = createScalingGalleryProcessor();
                    }

                    final Node imageRoot = session.getNode(event.nodePath());
                    final Node original = imageRoot.getNode(HippoGalleryNodeType.IMAGE_SET_ORIGINAL);

                    @SuppressWarnings("unchecked")
                    final Map<String, String> variantNamesToTypes = event.variants();

                    for (final String name : variantNamesToTypes.keySet()) {
                        final String type = variantNamesToTypes.get(name);
                        log.debug("--> creating image variant {} of type {}", name, type);

                        final Node variantNode = imageRoot.addNode(name, type);
                        stream = original.getProperty(JcrConstants.JCR_DATA).getBinary().getStream();
                        scalingProcessor.initGalleryResource(variantNode, stream, event.mimeType(), event.fileName(), Calendar.getInstance());
                    }
                    session.save();

                } catch (final PathNotFoundException e) {
                    log.debug("Image root (or 'original' subnode) not found, will retry again");
                    throw e;
                } catch (RepositoryException e) {
                    log.error(e.getClass().getName() + " during creation of variants for " + event.nodePath(), e);
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

    protected void retry(final Callable<Void> command) throws Exception {
        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        try {
            long delay = this.delay;
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
    protected long backoff(int attempt) {
        long duration = delay * (long) Math.pow(2, attempt);
        if (duration < 0) {
            duration = MAX_DELAY;
        }
        return Math.min(Math.max(duration, delay), MAX_DELAY);
    }

    protected void refresh() {
        try {
            session.refresh(false);
        } catch (RepositoryException e) {
            log.error("", e);
        }
    }

    @Override
    protected void doConfigure(final Node node) throws RepositoryException {
        maxRetry = getAsInteger(node, "maxRetry", DEFAULT_MAX_RETRY);
        delay = getAsInteger(node, "delay", DEFAULT_DELAY);
        log.debug("Reconfigured {}: maxRetry={}, delay={}", this.getClass().getName(), maxRetry, delay);
    }

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        HippoEventListenerRegistry.get().register(this);
    }

    @Override
    protected void doShutdown() {
        HippoEventListenerRegistry.get().unregister(this);
    }

    protected ScalingGalleryProcessor createScalingGalleryProcessor() throws RepositoryException {

        final BackgroundScalingGalleryProcessor processor = new BackgroundScalingGalleryProcessor();

        if (!session.nodeExists(GALLERY_PROCESSOR_SERVICE_PATH)) {
            log.error("Cannot access default gallery processor path at {}, skipping reading the parameters",GALLERY_PROCESSOR_SERVICE_PATH);
            return processor;
        }
        final Node config = session.getNode(GALLERY_PROCESSOR_SERVICE_PATH);

        final NodeIterator nodes = config.getNodes();
        while (nodes.hasNext()) {
            final Node scaleConfig = nodes.nextNode();

            final String nodeName = scaleConfig.getName();
            final int width = getAsInteger(scaleConfig, BackgroundScalingGalleryProcessorPlugin.CONFIG_PARAM_WIDTH, BackgroundScalingGalleryProcessorPlugin.DEFAULT_WIDTH);
            final int height = getAsInteger(scaleConfig, BackgroundScalingGalleryProcessorPlugin.CONFIG_PARAM_HEIGHT, BackgroundScalingGalleryProcessorPlugin.DEFAULT_HEIGHT);
            final boolean upscaling = JcrUtils.getBooleanProperty(scaleConfig, BackgroundScalingGalleryProcessorPlugin.CONFIG_PARAM_UPSCALING, BackgroundScalingGalleryProcessorPlugin.DEFAULT_UPSCALING);
            final float compressionQuality = (float) getAsDouble(scaleConfig, BackgroundScalingGalleryProcessorPlugin.CONFIG_PARAM_COMPRESSION, BackgroundScalingGalleryProcessorPlugin.DEFAULT_COMPRESSION);
            final String strategyName = JcrUtils.getStringProperty(scaleConfig, BackgroundScalingGalleryProcessorPlugin.CONFIG_PARAM_OPTIMIZE, BackgroundScalingGalleryProcessorPlugin.DEFAULT_OPTIMIZE);

            ImageUtils.ScalingStrategy strategy = BackgroundScalingGalleryProcessorPlugin.SCALING_STRATEGY_MAP.get(strategyName);
            if (strategy == null) {
                log.warn("Image variant '{}' specifies an unknown scaling optimization strategy '{}'. Possible values are {}. Falling back to '{}' instead.",
                        nodeName, strategyName, BackgroundScalingGalleryProcessorPlugin.SCALING_STRATEGY_MAP.keySet(), BackgroundScalingGalleryProcessorPlugin.DEFAULT_OPTIMIZE);
                strategy = BackgroundScalingGalleryProcessorPlugin.SCALING_STRATEGY_MAP.get(BackgroundScalingGalleryProcessorPlugin.DEFAULT_OPTIMIZE);
            }

            final ScalingParameters parameters = new ScalingParameters(width, height, upscaling, strategy, compressionQuality);
            log.debug("Scaling parameters for {}: {}", nodeName, parameters);
            processor.addScalingParameters(nodeName, parameters);
        }

        return processor;
    }

    protected double getAsDouble(final Node node, final String property, final double defaultValue) throws RepositoryException {
        if (node.hasProperty(property)) {

            return node.getProperty(property).getDouble();
        }
        return defaultValue;
    }

    protected int getAsInteger(final Node node, final String property, final int defaultValue) throws RepositoryException {
        if (node.hasProperty(property)) {

            return (int) node.getProperty(property).getLong();
        }
        return defaultValue;
    }

}
