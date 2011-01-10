package net.sf.helpaddons.crosslinkmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;

public class PoolRegistry {

    private final Map<String, List<String>> lookUpMap =
        new HashMap<String, List<String>>();

    private IStaticHelpContent helpContent =
        IStaticHelpContent.DEFAULT;

    public void setHelpContentDelegate(IStaticHelpContent helpContent) {
        this.helpContent = helpContent == null
                           ? IStaticHelpContent.DEFAULT
                           : helpContent;
    }

    public void changed(Set<IExtension> contentPoolsExtensions) {
        Map<String, List<String>> bundlesOfPool =
            new HashMap<String, List<String>>();
        Set<String> pools = new HashSet<String>();

        Map<String, List<String>> preferredBundlesReverse =
            new HashMap<String, List<String>>();
        for (IExtension ext : contentPoolsExtensions) {
            String bundleSymbolicName = ext.getNamespaceIdentifier();
            IConfigurationElement[] poolElements = ext.getConfigurationElements();
            for (IConfigurationElement element : poolElements) {
                if (!"pool".equals(element.getName())) continue;

                String pool = element.getAttribute("id"); //$NON-NLS-1$
                pools.add(pool);

                // bundles of pool: pool -joins-> bundles
                List<String> bundlesSet = bundlesOfPool.get(pool);
                if (bundlesSet == null) {
                    bundlesSet = new ArrayList<String>();
                    bundlesOfPool.put(pool, bundlesSet);
                }
                bundlesSet.add(bundleSymbolicName);

                // preferred bundles
                String bundlesToPrefer =
                    element.getAttribute("bundlesToPrefer"); //$NON-NLS-1$
                if (bundlesToPrefer != null) {
                    List<String> preferredList =
                        preferredBundlesReverse.get(bundleSymbolicName);
                    if (preferredList == null) {
                        preferredList = new ArrayList<String>();
                        preferredBundlesReverse.put(bundleSymbolicName, preferredList);
                    }
                    String[] splitted =
                        bundlesToPrefer.split("\\s*,\\s*"); //$NON-NLS-1$
                    for (String string : splitted) {
                        preferredList.add(0, string);
                    }
                }
            }
        }

        synchronized (this) {
            lookUpMap.clear();
            for (String pool : pools) {
                List<String> bundlesInSamePool = bundlesOfPool.get(pool);
                for (String bundle : bundlesInSamePool) {

                    // for this bundle does look-up already exist?
                    List<String> bundleLookUp = lookUpMap.get(bundle);
                    if (bundleLookUp == null) {
                        bundleLookUp = new ArrayList<String>();
                        lookUpMap.put(bundle, bundleLookUp);
                    }

                    // add all pool bundles except the bundle itself
                    for (String otherBundle : bundlesInSamePool) {
                        if (   !otherBundle.equals(bundle)
                                && !bundleLookUp.contains(otherBundle)) {
                            bundleLookUp.add(otherBundle);
                        }
                    }

                }
            }

            // sort according to "bundlesToPrefer" attribute
            for (String bundle : preferredBundlesReverse.keySet()) {
                List<String> listToSort = lookUpMap.get(bundle);
                if (listToSort == null) continue;

                for (String toPrefer : preferredBundlesReverse.get(bundle)) {
                    if (!listToSort.contains(toPrefer)) continue;
                    listToSort.remove(toPrefer);
                    listToSort.add(0, toPrefer);
                }

            }
        }

    }

    public IHrefResolver createHrefResolver(String sourceBundle,
                                            String sourceHref,
                                            Locale locale) {
        List<String> lookUpList;
        synchronized (this) {
            lookUpList = lookUpMap.get(sourceBundle);
        }
        if (lookUpList == null) {
            lookUpList = Collections.emptyList();
        }
        return new MyHrefResolver(sourceBundle,
                                  sourceHref,
                                  locale,
                                  lookUpList,
                                  helpContent);
    }

    private static class MyHrefResolver extends AbstractHrefResolver {

        private final String sourceBundle;
        private final Locale locale;
        private final List<String> lookUpList;
        private final IStaticHelpContent helpContent;

        public MyHrefResolver(String sourceBundle,
                              String sourceHref,
                              Locale locale,
                              List<String> lookUpList,
                              IStaticHelpContent helpContent) {
            super(sourceHref);
            this.sourceBundle = sourceBundle;
            this.locale = locale;
            this.lookUpList = lookUpList;
            this.helpContent = helpContent;
        }

        @Override
        protected boolean computeExistsInSourceBundle(String href) {
            return helpContent.checkExists(sourceBundle, href, locale);
        }

        @Override
        protected String computeTargetBundle(String href) {
            for (String bundle : lookUpList) {
                if (helpContent.checkExists(bundle, href, locale)) {
                    return bundle;
                }
            }
            return null;
        }

        @Override
        protected String getNotFoundHtmlFile() {
            return "error404.htm";
        }

        public String getNotFoundClassName() {
            return "error404";
        }

    }

}
