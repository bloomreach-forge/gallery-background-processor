package org.onehippo.cms7.gallery;

import org.apache.commons.io.IOUtils;
import org.hippoecm.editor.type.PlainJcrTypeStore;
import org.hippoecm.frontend.editor.plugins.resource.ResourceHelper;
import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.plugins.gallery.imageutil.ImageBinary;
import org.hippoecm.frontend.plugins.gallery.imageutil.ScalingParameters;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.frontend.plugins.gallery.processor.ScalingGalleryProcessor;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.gallery.HippoGalleryNodeType;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class BackgroundScalingGalleryProcessor extends ScalingGalleryProcessor {


    private static final Logger log = LoggerFactory.getLogger(BackgroundScalingGalleryProcessor.class);


    @Override
    public ScalingParameters addScalingParameters(final String nodeName, final ScalingParameters parameters) {
        return super.addScalingParameters(nodeName, parameters);
    }


    @Override
    public void makeImage(Node node, InputStream stream, String mimeType, String fileName) throws GalleryException,
            RepositoryException {
        long time = System.currentTimeMillis();

        Node resourceNode = getPrimaryChild(node);
        if (!resourceNode.isNodeType(HippoNodeType.NT_RESOURCE)) {
            throw new GalleryException("Resource node not of primaryType " + HippoNodeType.NT_RESOURCE);
        }

        //Create a new image binary that serves as the original source converted to RGB plus image metadata
        ImageBinary image = new ImageBinary(node, stream, fileName, mimeType);

        log.debug("Setting JCR data of primary resource");
        ResourceHelper.setDefaultResourceProperties(resourceNode, image.getMimeType(), image, image.getFileName());

        //TODO: Currently the InputStream is never used in our impls, might revisit this piece of the API
        InputStream isTemp = image.getStream();
        try {
            //store the filename in a property
            initGalleryNode(node, isTemp, image.getMimeType(), image.getFileName());
        } finally {
            IOUtils.closeQuietly(isTemp);
        }

        IStore<ITypeDescriptor> store = new PlainJcrTypeStore(node.getSession());
        ITypeDescriptor type;
        try {
            type = store.load(node.getPrimaryNodeType().getName());
        } catch (StoreException e) {
            throw new GalleryException("Could not load primary node type of " + node.getName() + ", cannot create imageset variants", e);
        }

        //create the primary resource node
        log.debug("Creating primary resource {}", resourceNode.getPath());
        Calendar lastModified = resourceNode.getProperty(JcrConstants.JCR_LAST_MODIFIED).getDate();
        initGalleryResource(resourceNode, image.getStream(), image.getMimeType(), image.getFileName(), lastModified);
        final Map<String, String> variants = new HashMap<>();
        // create all resource variant nodes
        for (IFieldDescriptor field : type.getFields().values()) {
            if (field.getTypeDescriptor().isType(HippoGalleryNodeType.IMAGE)) {
                String variantPath = field.getPath();
                if (!node.hasNode(variantPath)) {
                    // always create original and thumbnail
                    if (variantPath.equals(HippoGalleryNodeType.IMAGE_SET_ORIGINAL) || variantPath.equals(HippoGalleryNodeType.IMAGE_SET_THUMBNAIL)) {

                        log.debug("creating variant resource {}", variantPath);
                        final Node variantNode = node.addNode(variantPath, field.getTypeDescriptor().getType());
                        initGalleryResource(variantNode, image.getStream(), image.getMimeType(), image.getFileName(), lastModified);

                    } else {
                        log.debug("Scheduling variant for background processing: {}", variantPath);
                        variants.put(variantPath, field.getTypeDescriptor().getType());
                    }
                }
            }

        }
        final HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
        for (Map.Entry<String, String> entry : variants.entrySet()) {
            if (eventBus != null) {
                final String variant = entry.getKey();
                final ImageVariantEvent variantEvent = new ImageVariantEvent(variant)
                        .node(node.getPath())
                        .type(entry.getValue())
                        .mimeType(image.getMimeType())
                        .fileName(image.getFileName())
                        .variant(variant);
                variantEvent.sealEvent();
                eventBus.post(variantEvent);
            }
        }


        image.dispose();

        if (log.isDebugEnabled()) {
            time = System.currentTimeMillis() - time;
            log.debug("Processing image '{}' took {} ms.", fileName, time);
        }
    }


}
