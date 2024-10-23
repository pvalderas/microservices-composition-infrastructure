# Microservice composition based on the Choreography of BPMN fragments. Infrastructure

This repositry contains a software infrastructure to create a choreographed composition of microservices by using BPMN diagrams. 
An example using this infrastructure is available in the following Github repository: [microservices-composition-example](https://github.com/pvalderas/microservices-composition-example).

It has been implemented by using:

* [Spring Boot](https://spring.io/projects/spring-boot)
* [Netflix infrastructure](https://github.com/Netflix)
* [Camunda Engine](https://github.com/camunda/camunda-bpm-spring-boot-starter)
* [BPMN.io](https://github.com/bpmn-io)
* [RabbitMQ](https://www.rabbitmq.com/)

# About

This is the result of a research work leaded by Pedro Valderas at the PROS Research Center, Universitat Politècnica de València, Spain.

This work presents a microservice composition approach based on the choreography of BPMN fragments. On the one hand, developers can describe the big picture of the composition with a BPMN model, providing a valuable mechanism to analyse it when engineering decisions need to be taken. On the other hand, this model is split into fragments in order to be executed though an event-based choreography form, providing the high degree of decoupling among microservices demanded in this type of architecture. 

This composition approach is supported by a microservice infrastucture developed to achieve that both descriptions of a composition (big picture and split one) coexist. In this sense, microservices compositions can be evolved graphically from both either the BPMN descrition of the big picture or a BPMN fragment.

# The proposed architecture

The microservice architecture to support the choreographed composition of microservices through BPMN fragments is shown below.

![architecture](./architecture.png "Proposed Architecture") 

The architectural elements that support our proposal are depicted in red. Thery are the following:

* Business microservices are complemented with a Compositon Coordinator in such a way a business microservice can be considered as the assembly of two main elements:  The Composition Coordinator, which is in charge of interpreting BPMN fragments in order to execute tasks and interact with other microservices; and the backend, which implements the functionality required to execute the tasks of each microservice. The Composition Coordinator endows each microservice with a PMN Editor based on BPMN.io in order to modify its BPMN fragment.
* The Global Composition Manager microservice, which is in charge of managing the big picture of a microservice composition. It stores the BPMN model that describes the complete composition. It also updates it when a microservice evolves its corresponding fragment. In addition, it is in charge of sending each composition to the Fragment Manager. The Global Manager is complemented with a BPMN Editor based on BPMN.io in order to create microservice compositions.
* The Fragment Manager microservice, which plays the role of gateway between the Global Composition Manager and the Composition Coordinator of each microservice. It is in charge of splitting a global BPMN composition into fragments, and distribute these fragments among the different Composition Coordinators.

# Creating a Global Composition Manager

To create a Global Composition Manager you can use Gradle to build the corresponding project in this repository and include it as a dependency of a Spring Boot Application. Then, you just need to annotate the main class with the ```@GlobalCompositionManager``` as presented bellow. Note that the ```@SpringBootApplication``` annotation must be configured to find beans in the ```es.upv.pros.pvalderas.globalcompositionmanager``` package.

```java
@GlobalCompositionManager
@SpringBootApplication(scanBasePackages = {"es.upv.pros.pvalderas.globalcompositionmanager"})
public class GlobalCompositionManagerMicroservice {
	public static void main(String[] args) {
		SpringApplication.run(GlobalCompositionManagerMicroservice.class, args);
	}
}
```
Next, you must create an application.yml file, indicating the urls of both the Fragment Manager, and the Service Registry in which microservice are registered. Currently, only Netflix Eureka is supported as Service Registry. Other registries will be supported further.

```yml
server:
  port: 8084

composition:
  fragmentmanager:
    url: http://localhost:8083/compositions
  serviceregistry:
    url: http://localhost:9999/eureka-server
    type: eureka
```
 
# Creating a Fragment Manager

To create a Global Composition Manager you can use Gradle to build the corresponding project in this repository and include it as a dependency of a Spring Boot Application. Then, you just need to annotate the main class with the ```@FragmentManager``` as presented bellow. Note that the ```@SpringBootApplication``` annotation must be configured to find beans in the ```es.upv.pros.pvalderas.fragmentmanager``` package.

```java
@FragmentManager
@SpringBootApplication(scanBasePackages = {"es.upv.pros.pvalderas.fragmentmanager"})
public class FragmentManagerMicroservice {
	public static void main(String[] args) {
		SpringApplication.run(FragmentManagerMicroservice.class, args);
	}
}
```
Next, you must create an application.yml file, indicating the url of the Global Composition Manager.

```yml
server:
  port: 8083

composition:
  globalcompositionmanager:
    url: http://localhost:8084
```

# Creating a business microservice extended with a Compositon Coordinator

To create a domain microservice extended with the functionality of a Composition Coordinator you can use Gradle to build the corresponding project in this repository and include it as a dependency of a Spring Boot Application. Then, you just need to annotate the main class with the ```@CompositionCoordinator``` as presented bellow. Note that the ```@SpringBootApplication``` annotation must be configured to find beans in the ```es.upv.pros.pvalderas.compositioncoordinator``` package as well as the package in which the HTTP controller of the microservice is implemented (```es.upv.pros.pvalderas.composition.example.customers``` in the example below). In addition, the ```@CompositionCoordinator``` annotation must be configured with the class object of the microservice HTTP controller.

```java
@EnableDiscoveryClient
@CompositionCoordinator(serviceAPIClass=CustomersHTTPController.class)
@SpringBootApplication(scanBasePackages = {"es.upv.pros.pvalderas.compositioncoordinator","es.upv.pros.pvalderas.composition.example.customers"})
public class Customers {
	public static void main(String[] args) {
		SpringApplication.run(Customers.class, args);
	}	
}
```
Next, you must create an application.yml file, indicating the following data:

* name of the microservice, which is shown in the BPMN editor
* Connection data of the message broker. Currently, only RabbitMQ is supported.
* The url of the Fragment Manager
* The configuration requested by the Service Resgistry. Currently, only Eureka is supported.

```yml
spring:
  application:
    name: Customers
    
server:
  port: 8081
  
composition:
  messagebroker:
    type: rabbitmq
    host: localhost
    port: 5672
    exchange: composition
  fragmentmanager:
    url: http://localhost:8083
    
eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:2222/eureka
```
# Using the infrastructure to create and execute a microservice composition

In [microservices-composition-example](https://github.com/pvalderas/microservices-composition-example) you can find the implementation of a case study based on the process of purchase orders. In this example, it is explained how using the BPMN editor of the Global Composition Manager, how executing the composition, and how evolving a composition from both big picture created with the Global Composition Manager and the BPMN fragments available in each microservice.

#Knowledgment

Grant MCIN/AEI/10.13039/501100011033 funded by: ![mcin](./mcin.png "Ministeria de Cienca e innovación"|width=25) ![aie](./aei.png "Agencia Estatal de Investigación"|width=25) 
