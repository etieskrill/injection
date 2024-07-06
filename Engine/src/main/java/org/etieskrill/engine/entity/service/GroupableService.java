package org.etieskrill.engine.entity.service;

import java.util.ArrayList;
import java.util.List;

public abstract class GroupableService implements Service {

    private final List<Service> preServices;
    private final List<Service> postServices;

    protected GroupableService() {
        this.preServices = new ArrayList<>();
        this.postServices = new ArrayList<>();
    }

    public List<Service> getPreServices() {
        return preServices;
    }

    public void addPreService(Service service) {
        preServices.add(service);
    }

    public List<Service> getPostServices() {
        return postServices;
    }

    public void addPostService(Service service) {
        postServices.add(service);
    }

    public void addService(Service service) {
        addPostService(service);
    }

    public void clearServices() {
        preServices.clear();
        postServices.clear();
    }

}
