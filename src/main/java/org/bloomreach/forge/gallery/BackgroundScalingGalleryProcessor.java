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

import org.hippoecm.frontend.plugins.gallery.imageutil.ImageBinary;
import org.hippoecm.frontend.plugins.gallery.imageutil.ScalingParameters;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.gallery.processor.ScalingGalleryProcessor;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.repository.gallery.HippoGalleryNodeType;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * BackgroundScalingGalleryProcessor that:
 * - creates original and thumbnail directly within the upload
 * - posts the other variants to the event bus, for BackgroundGalleryProcessorModule to pick up
 */
public class BackgroundScalingGalleryProcessor extends ScalingGalleryProcessor {

    private static final Logger log = LoggerFactory.getLogger(BackgroundScalingGalleryProcessor.class);

    @Override
    public ScalingParameters addScalingParameters(final String nodeName, final ScalingParameters parameters) {
        return super.addScalingParameters(nodeName, parameters);
    }

    @Override
    protected void initGalleryResourceVariants(final Node node, final ImageBinary image, final ITypeDescriptor type,
                                               final Calendar lastModified) throws RepositoryException, GalleryException {

        final HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
        if (eventBus == null) {
            log.warn("Hippo Event Bus not found: cannot send image variant creation event. Falling back to default behavior of {}", ScalingGalleryProcessor.class.getName());
            super.initGalleryResourceVariants(node, image, type, lastModified);
            return;
        }

        final Map<String, String> backgroundImageVariants = new HashMap<>();

        // create only some resource variant nodes, collect the others
        for (IFieldDescriptor field : type.getFields().values()) {
            if (field.getTypeDescriptor().isType(HippoGalleryNodeType.IMAGE)) {
                final String variantPath = field.getPath();
                if (!node.hasNode(variantPath)) {
                    // create some variants directly, keep the others to be posted as event
                    if (isBackgroundImageVariant(variantPath)) {
                        log.debug("Scheduling variant {} for background processing", variantPath);
                        backgroundImageVariants.put(variantPath, field.getTypeDescriptor().getType());
                    } else {
                        log.debug("Directly creating variant {} ", variantPath);
                        final Node variantNode = node.addNode(variantPath, field.getTypeDescriptor().getType());
                        initGalleryResource(variantNode, image.getStream(), image.getMimeType(), image.getFileName(), lastModified);
                    }
                }
            }
        }

        // post the other variants to the event bus, for BackgroundGalleryProcessorModule to pick up
        final ImageCreationEvent variantEvent = new ImageCreationEvent(UserSession.get().getApplicationName())
                .nodePath(node.getPath())
                .mimeType(image.getMimeType())
                .fileName(image.getFileName())
                .variants(backgroundImageVariants);
        variantEvent.sealEvent();
        eventBus.post(variantEvent);
    }

    /**
     * Is a certain variant to be created now or to be created in the background?
     */
    protected boolean isBackgroundImageVariant(final String variantPath) {
        // any other than original or thumbnail
        return !(variantPath.equals(HippoGalleryNodeType.IMAGE_SET_ORIGINAL) || variantPath.equals(HippoGalleryNodeType.IMAGE_SET_THUMBNAIL));
    }
}
