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

    private final Map<String, String> defaultErrorPages =
        new HashMap<String, String>();

    private final Map<String, Map<String, String>> specificErrorPages =
        new HashMap<String, Map<String, String>>();

    private IStaticHelpContent helpContent =
        IStaticHelpContent.DEFAULT;

    public void setHelpContentDelegate(IStaticHelpContent helpContent) {
        this.helpContent = helpContent == null
                           ? IStaticHelpContent.DEFAULT
                           : helpContent;
    }

    public void changed(Set<IExtension> contentPoolsExtensions) {
        Map<String, String> tempDefaultErrorPages =
            new HashMap<String, String>();
        Map<String, Map<String, String>> tempSpecifiErrorPages =
            new HashMap<String, Map<String, String>>();

        Map<String, List<String>> bundlesOfPool =
            new HashMap<String, List<String>>();
        Set<String> pools = new HashSet<String>();

        Map<String, List<String>> preferredBundlesReverse =
            new HashMap<String, List<String>>();
        for (IExtension ext : contentPoolsExtensions) {
            String bundleSymbolicName = ext.getNamespaceIdentifier();
            IConfigurationElement[] childElements = ext.getConfigurationElements();
            for (IConfigurationElement element : childElements) {
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

            // error pages
            for (IConfigurationElement element : childElements) {
                if (!"errorPage".equals(element.getName())) continue;

                String href =
                    element.getAttribute("href"); //$NON-NLS-1$
                if (href == null) continue;
                String classPrefix =
                    element.getAttribute("classPrefix"); //$NON-NLS-1$

                // default error page
                if (classPrefix == null) {
                    tempDefaultErrorPages.put(bundleSymbolicName, href);
                    continue;
                }

                // specific error page
                Map<String, String> specifics =
                    tempSpecifiErrorPages.get(bundleSymbolicName);
                if (specifics == null) {
                    specifics = new HashMap<String, String>();
                    tempSpecifiErrorPages.put(bundleSymbolicName, specifics);
                }
                specifics.put(classPrefix, href);
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
            for (Map.Entry<String, List<String>> entry : preferredBundlesReverse.entrySet()) {

                List<String> listToSort = lookUpMap.get(entry.getKey());
                if (listToSort == null) continue;

                for (String toPrefer : entry.getValue()) {
                    if (!listToSort.contains(toPrefer)) continue;
                    listToSort.remove(toPrefer);
                    listToSort.add(0, toPrefer);
                }
            }

            // error pages
            defaultErrorPages.clear();
            defaultErrorPages.putAll(tempDefaultErrorPages);
            specificErrorPages.clear();
            specificErrorPages.putAll(tempSpecifiErrorPages);
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

        String errorPage = defaultErrorPages.get(sourceBundle);
        return new MyHrefResolver(sourceBundle,
                                  sourceHref,
                                  locale,
                                  errorPage == null ? "error404.htm" : errorPage,
                                  specificErrorPages.get(sourceBundle),
                                  lookUpList,
                                  helpContent);
    }

    private static class MyHrefResolver extends AbstractHrefResolver {

        private final String sourceBundle;
        private final Locale locale;
        private final String defaultErrorPage;
        private final Map<String, String> errorPages;
        private final List<String> lookUpList;
        private final IStaticHelpContent helpContent;

        public MyHrefResolver(String sourceBundle,
                              String sourceHref,
                              Locale locale,
                              String defaultErrorPage,
                              Map<String, String> errorPages,
                              List<String> lookUpList,
                              IStaticHelpContent helpContent) {
            super(sourceHref);
            this.sourceBundle = sourceBundle;
            this.locale = locale;
            this.defaultErrorPage = defaultErrorPage;
            this.errorPages = errorPages;
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
        protected String getNotFoundHtmlFile(String classPrefix) {
            if (classPrefix == null || errorPages == null)
                return defaultErrorPage;

            String result = errorPages.get(classPrefix);
            return result == null ? defaultErrorPage : result;
        }

        public String getNotFoundClassName() {
            return "error404";
        }

    }

}
