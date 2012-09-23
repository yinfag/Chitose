package ru.yinfag.chitose;



public interface Plugin {

	void setProperty(String name, String domain, String value);

	void init();
    
    void shutdown();
}
