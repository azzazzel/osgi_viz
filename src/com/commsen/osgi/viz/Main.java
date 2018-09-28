package com.commsen.osgi.viz;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.dto.FrameworkDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

public class Main {
 
	public static void main(String[] args) throws ClassNotFoundException, IOException, TemplateException, BundleException {
 
		Render modulesView = new Render();
		Render servicesView = new Render();
		Render packagesView = new Render();
		Render allView = new Render();
		
		int baseModules = 0, modules = 0, serviceDefs = 0, services = 0;

		final Path OSGI_STORAGE = FileSystems.getDefault().getPath("/tmp/OSGi/cache");
		if (OSGI_STORAGE.toFile().exists()) {
			Files.walk(OSGI_STORAGE).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		}
        Map<String, String> osgiConfig = new HashMap<>();
        osgiConfig.put(Constants.FRAMEWORK_STORAGE, OSGI_STORAGE.toString());
        osgiConfig.put(Constants.FRAMEWORK_STORAGE_CLEAN, "true");
        
        
        Framework framework = null;
        try {
            FrameworkFactory frameworkFactory = ServiceLoader.load(FrameworkFactory.class)
                    .iterator().next();
            framework = frameworkFactory.newFramework(osgiConfig);
            framework.start();
            
            BundleContext bundleContext = framework.getBundleContext();

    		final Path MODULES = FileSystems.getDefault().getPath("modules");
            
    		Files.walk(MODULES).map(Path::toFile).filter(f -> f.isFile()).forEach(f -> {
            	System.out.println(f);
                Bundle b;
				try {
					b = bundleContext.installBundle("file:" + f.getAbsolutePath());
					b.start();
				} catch (BundleException e) {
					e.printStackTrace();
				}
    		});
            
            for (Bundle bundle : bundleContext.getBundles()) {
    			modules++;
    			System.out.println("Processing " + bundle.getSymbolicName() + " STATE: " + bundle.getState());
    			Node node = new Node();
    			node.id = "m." + bundle.getBundleId();
    			node.name = bundle.getSymbolicName();
    			node.type = "Module";
    			modulesView.nodes.add(node);
    			servicesView.nodes.add(node);	
    			packagesView.nodes.add(node);
    			allView.nodes.add(node);

    			for (Capability capability : bundle.adapt(BundleRevision.class).getCapabilities("osgi.wiring.package")) {
    				String name = capability.getAttributes().get("osgi.wiring.package").toString();
					node = new Node();
					node.id = "p." + name;
					node.name = name;
					node.type = "Package";
					packagesView.nodes.add(node);
					allView.nodes.add(node);
	
					Link link = new Link();
					link.from = "m." + bundle.getBundleId();
					link.to = "p." + name;
					link.label = "Exports";
					packagesView.links.add(link);
					allView.links.add(link);
				}
				for (Requirement requirement : bundle.adapt(BundleRevision.class).getRequirements("osgi.wiring.package")) {
					if (requirement.getAttributes() != null) {
		            	Pattern pattern = Pattern.compile("osgi.wiring.package=([^\\)]*)");
		            	Matcher m = pattern.matcher(requirement.getDirectives().get("filter"));
		            	m.find();
		            	
						Link link = new Link();
						link.to = "m." + bundle.getBundleId();
						link.from = "p." + m.group(1);
						link.label = "Requires";
						packagesView.links.add(link);
						allView.links.add(link);
					}
				}
			}
            
			List<ServiceReferenceDTO> serviceReferenceDTOs = framework.adapt(FrameworkDTO.class).services;
			for (ServiceReferenceDTO serviceReferenceDTO : serviceReferenceDTOs) {
				for (String serviceName : (String[])serviceReferenceDTO.properties.get("objectClass")) {
					serviceDefs++;
					Node node = new Node();
					node.id = "sd." + serviceName;
					node.name = serviceName;
					node.type = "Service definition";
					servicesView.nodes.add(node);
					allView.nodes.add(node);
	
					Link link = new Link();
					link.from = "m." + serviceReferenceDTO.properties.get("service.bundleid");
					link.to = "sd." + serviceName;
					link.label = "Provides";
					servicesView.links.add(link);
					allView.links.add(link);	
					
					for (long b : serviceReferenceDTO.usingBundles) {
						link = new Link();
						link.from = "sd." + serviceName;
						link.to = "m." + b;
						link.label = "Uses";
						servicesView.links.add(link);
						allView.links.add(link);
					}
				}
			}
		} catch (Exception ex) {
            System.out.println("OSGi Failed to Start");
            ex.printStackTrace();
        } finally {
            framework.stop();
		}
        
		System.out.println("All modules: " + modules);
		System.out.println("Base modules: " + baseModules);
		System.out.println("Service definitions: " + serviceDefs);
		System.out.println("Services: " + services);
		
		Configuration cfg = new Configuration(Configuration.VERSION_2_3_27);
		BeansWrapperBuilder wrapperBuilder = new BeansWrapperBuilder(Configuration.VERSION_2_3_27);
		wrapperBuilder.setExposeFields(true);
		
		cfg.setObjectWrapper(wrapperBuilder.build());
		cfg.setDirectoryForTemplateLoading(new File("."));
		cfg.setDefaultEncoding("UTF-8");
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		cfg.setLogTemplateExceptions(false);
		cfg.setWrapUncheckedExceptions(true);
		
		
		Template temp = cfg.getTemplate("nodes.json.ftl");

		Map<String, Object> root = new HashMap<>();
		root.put("nodes", servicesView.nodes);
		root.put("links", servicesView.links);
		temp.process(root, new FileWriter(new File("html/services.json")));

		root.put("nodes", modulesView.nodes);
		root.put("links", modulesView.links);
		temp.process(root, new FileWriter(new File("html/modules.json")));

		root.put("nodes", packagesView.nodes);
		root.put("links", packagesView.links);
		temp.process(root, new FileWriter(new File("html/packages.json")));

		root.put("nodes", allView.nodes);
		root.put("links", allView.links);
		temp.process(root, new FileWriter(new File("html/all.json")));
	}

}		
	