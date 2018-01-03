package project;
import static java.lang.Math.abs;
import java.util.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.Color;
import java.awt.Graphics2D;


/**
 * Created by eva on 11/26/17.
 */
public class VRPComponents {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        //Set up Input for TSP
        //number of customers
        int numberOfCustomers = 30;
        //vehicle capacity
        int vehicleCapacity = 50;
        //vehicle number
        int vehicleNo = 0;
        //total number of vehicles
        int totalVehicles = 10;
        //number of intra route relocations
        int intraLCNo =0;
        //number of inter route relocations
        int interLCNo =0;
        //total number of routes
        int noRoutes =0;

        //VRP greedy solution total cost before relocations
        double totalCostBRC = 0;
        //solution total cost after intra relocations
        double intraTC =0;
        double totalCostAfterIntra=0;
        //solution total cost after inter relocations
        double interTC =0;

        //solution load
        double solutionLoad=0;
        //total load after intra
        double totalLoadAfterIntra=0;

        //Create the depot
        project.Node depot = new project.Node();
        depot.x = 50;
        depot.y = 50;
        depot.demand = 0;

        //Create the list with the customers
        ArrayList<project.Node> customers = new ArrayList<project.Node>();

        //randomly fill in the customers' information (coordinates, demand and ID)
        Random ran = new Random(160894);
        for (int i = 1; i <= numberOfCustomers; i++) {
            project.Node cust = new project.Node();

            cust.x = ran.nextInt(100);
            cust.y = ran.nextInt(100);
            cust.demand = 4 + ran.nextInt(7);
            cust.ID = i;
            customers.add(cust);
        }

        //Build the allNodes array and the corresponding distance matrix
        ArrayList<project.Node> allNodes = new ArrayList<project.Node>();

        allNodes.add(depot);
        for (int i = 0; i < customers.size(); i++) {
            project.Node cust = customers.get(i);
            allNodes.add(cust);
        }

        for (int i = 0; i < allNodes.size(); i++) {
            project.Node nd = allNodes.get(i);
            nd.ID = i;
        }

        // This is a 2-D array which will hold the distances between node pairs
        // The [i][j] element of this array is the distance required for moving
        // from the i-th node of allNodes (node with id : i)
        // to the j-th node of allNodes list (node with id : j)
        double[][] distanceMatrix = new double[allNodes.size()][allNodes.size()];
        for (int i = 0; i < allNodes.size(); i++) {
            project.Node from = allNodes.get(i);


            //for (int j = 0; j <i; j++) {
            for (int j = 0;  j < allNodes.size(); j++) {

                project.Node to = allNodes.get(j);

                double Delta_x = (from.x - to.x);
                double Delta_y = (from.y - to.y);
                double distance = Math.sqrt((Delta_x * Delta_x) + (Delta_y * Delta_y));

                distance = Math.round(distance);

                distanceMatrix[i][j] = distance;

            }
        }

        //Nearest Neighbor
        //iterative procedure: at each iteration insert the nearest neighbor into the solution

        // This is the solution object - It will store the solution as it is iteratively generated
        // The constructor of Solution class will be executed
        project.Solution s = new project.Solution();
        project.Solution sAfterIntraRelocation = new project.Solution();
        project.Solution sAfterInterRelocation = new project.Solution();

        //the solution object contains an arraylist rt with route objects
        //the solution contains all the routes with the route costs and route loads
        //the each route object describes one route
        project.Route route = new project.Route();

        //customer demand
        double custDemand = 0.00;
        double custDemand2 =  0.00;
        //count the number of customers which are being inserted into the solution
        int cntCust = 0;
        //complete will be true when the greedy VRP solution finishes and when all inter and intra relocations are completed
        boolean complete = false;
        //succesfulintralocation willbe true when all the intra relocations are completed
        boolean succesfulintralocation = false;
        //succesfulintralocation willbe true when all the inter relocations are completed
        boolean succesfulinterlocation = false;

        //candidate id
        int candID = 0;

        // Let nodeSequence be the arraylist of nodes contained in route (s.rt)
        // Let route be the one of the routes contained in s
        ArrayList<project.Node> nodeSequence = route.nodes;
        ArrayList<project.Node> allNodeSequence = new ArrayList<>();

        ArrayList<project.Route> routes = s.rt;


        // indicate that all customers are non-routed
        for (int i = 0; i < customers.size(); i++) {
            customers.get(i).isRouted = false;
        }

        //complete will be true when the greedy VRP solution finishes and when all inter and intra relocations are completed
        while (!complete) {
            if(!succesfulinterlocation) {
                // count number of vehicles used for the greedy vrp solution
                vehicleNo = vehicleNo + 1;
                System.out.println("\n   ################");
                System.out.println("### Route number "+vehicleNo+" ###");
                System.out.println("   ################");

                System.out.println("Customers:");

            }


            boolean newVehicle = false;

            if (!succesfulinterlocation) {

                //add the depot in the begining of the route
                nodeSequence.add(depot);

                //ITERATIVE BODY OF THE NN ALGORITHM
                //Q - How many insertions? A - Equal to the number of customers! Thus for i = 0 -> customers.size()
                for (int i = 0; i < customers.size(); i++) {

                    //this will be the position of the nearest neighbor customer -- initialization to -1
                    int positionOfTheNextOne = -1;

                    // This will hold the minimal cost for moving to the next customer - initialized to something very large
                    double bestCostForTheNextOne = Double.MAX_VALUE;

                    //This is the last customer of the route (or the depot if the route is empty)
                    Node lastInTheRoute = nodeSequence.get(nodeSequence.size() - 1);


                    //First Step: Identify the non-routed nearest neighbor (his position in the customers list) of the last node in the nodeSequence list
                    for (int j = 0; j < customers.size(); j++) {
                        // The examined node is called candidate
                        Node candidate = customers.get(j);
                        custDemand = candidate.demand;

                        // if this candidate has not been pushed in the solution
                        if (candidate.isRouted == false) {
                            //This is the cost for moving from the last to the candidate one
                            double trialCost = distanceMatrix[lastInTheRoute.ID][candidate.ID];
                            custDemand = candidate.demand;

                            //If this is the minimal cost found so far and if the vehicle can satisfy this customer's demand
                            //-> store this cost and the position of this best candidate
                            if (trialCost < bestCostForTheNextOne && !complete && vehicleNo <= totalVehicles && ((route.currentrouteload + custDemand) <= vehicleCapacity) && !newVehicle) {
                                positionOfTheNextOne = j;
                                bestCostForTheNextOne = trialCost;
                                candID = candidate.ID;
                                custDemand2 = candidate.demand;

                            //If this is the minimal cost found so far and but the vehicle is full
                            //-> move to the next vehicle
                            } else if (trialCost < bestCostForTheNextOne && !complete && vehicleNo <= totalVehicles && ((route.currentrouteload + custDemand) > vehicleCapacity) && !newVehicle) {
                                candID = candidate.ID;
                                newVehicle = true;

                            //If the vehicle is full and there are no other vehicles left to satisfy the customers' demand
                            //-> you have run out of vehicles
                            } else if (trialCost < bestCostForTheNextOne && !complete && vehicleNo == totalVehicles) {
                                System.out.println("Customer demand is too high !!!! You need more vehicles or more routes in order to satisfy all the customers");
                                System.exit(0);

                            }

                        }

                    }


                    // Step 2: Push the customer in the solution

                    // We have found the customer to be pushed!!!
                    // He is located in the positionOfTheNextOne position of the customers list
                    // Let's inert him and update the cost of the solution and of the route, accordingly
                    // Don't forget to indicate that this customer is now routed

                    // if the current vehicle can satisfy the customer's demand, push him into the solution
                    if (!newVehicle && positionOfTheNextOne != -1) {
                        //Give him a name
                        Node insertedNode = customers.get(positionOfTheNextOne);

                        //Push him
                        nodeSequence.add(insertedNode);
                        cntCust = cntCust + 1;

                        //print the candidate id
                        System.out.println(candID);

                        // update the cost of the solution!
                        // What is the cost augmentation???
                        // We have already found it!!! It is the bestCostForTheNextOne
                        // Let's see if we are correct
                        double testCost = distanceMatrix[lastInTheRoute.ID][insertedNode.ID];

                        if (testCost != bestCostForTheNextOne) {
                            //If we are not correct
                            System.out.println("Something has gone wrong with the cost calculations !!!!");
                        }
                        // update the total cost and the total load of the solution
                        s.totalCost = s.totalCost + bestCostForTheNextOne;
                        s.totalLoad = s.totalLoad + custDemand2;

                        // update the cost and the load of the current route
                        route.cost = route.cost + bestCostForTheNextOne;
                        route.currentrouteload = route.currentrouteload + custDemand2;


                        // Update the isRouted status for the ode just inserted in the solution
                        insertedNode.isRouted = true;

                        // bhke o neos pelaths sto route

                    }

                }

                // All customers have been pushed in this route

                // Now we have to push the depot in the final point of the route -- finalize tsp solution
                Node lastInTheRoute = nodeSequence.get(nodeSequence.size() - 1);
                nodeSequence.add(depot);

                //The cost is augmented by moving from the last customer back to the depot
                s.totalCost = s.totalCost + distanceMatrix[lastInTheRoute.ID][depot.ID];

                route.cost = route.cost + distanceMatrix[lastInTheRoute.ID][depot.ID];

                routes.add(route);

                // this is the total cost of the greedy vrp solution
                totalCostBRC = totalCostBRC+route.cost;

                // creates a png with the current greedy vrp route and its cost
                drawRoutes(s, route, allNodes, Integer.toString(vehicleNo));

                // print all the information for the current route
                System.out.println("Route cost: "+route.cost);
                System.out.println("Total cost: "+s.totalCost);
                System.out.println("Route load: "+route.currentrouteload);
                System.out.println("Total load: "+s.totalLoad);
                System.out.println("### End of Greedy VRP for route number "+vehicleNo);
                solutionLoad = s.totalLoad;

            }


            // the new route has been added into the solution
            // the depot has been added into the end of each route
            //END OF GREEDY VRP CODE
            //
            //The GREEDY VRP Solution has been generated
            //
            ////////////////////////////////////////////////////////////////////////////////////////////////////


            //START OF LOCAL SEARCH CODE/////////////////////////////////////////////////////////////////////////
            //
            //Local Search
            //
            // we will first perform intra route relocations
            //
            // after the intra route relocations are performed and the new solution (after intra) is stored
            // we will perform inter route relocations (from one route to another)

            // succesfulinterlocation=true means all intra relocations have been made and we will now examine all inter route relocations
            if(succesfulinterlocation){
                // route. cost=total cost after intra relocations
                route.cost = intraTC;

            }

            //this is a boolean flag (true/false) for terminating the local search procedure
            boolean terminationCondition = false;

            //this is a counter for holding the local search iterator
            int localSearchIterator = 1;

            int LOCAL_SEARCH_MODE = 0;

            //Here we apply the best relocation move local search scheme

            String state = "state"+localSearchIterator;

            if (LOCAL_SEARCH_MODE == 0)
            {
                //This is an object for holding the best relocation move that can be applied to the candidate solution
                RelocationMove rm = new RelocationMove();

                // Until the termination condition is set to true repeat the following block of code
                while (terminationCondition == false)
                {
                    //Initialize the relocation move rm
                    rm.positionOfRelocated = -1;
                    rm.positionToBeInserted = -1;
                    rm.moveCost = Double.MAX_VALUE;

                    //With this function we look for the best relocation move
                    //the characteristics of this move will be stored in the object rm
                    findBestRelocationMove(rm, route, s, distanceMatrix, succesfulinterlocation, vehicleNo, vehicleCapacity);

                    // If rm (the identified best relocation move) is a cost improving move, or in other words
                    // if the current solution is not a local optimum
                    if (rm.moveCost < 0)
                    {
                        //This is a function applying the relocation move rm to the candidate solution
                        if(!succesfulinterlocation){
                            //count the number of intra route relocations
                            intraLCNo = intraLCNo+1;

                        }else if(succesfulinterlocation){
                            //count the number of inter route relocations
                            interLCNo = interLCNo+1;

                        }

                        // apply relocation move
                        applyRelocationMove(rm, route, s, distanceMatrix);


                        if (succesfulinterlocation){
                            state= "afterInter"+localSearchIterator;
                        }else if (!succesfulinterlocation){
                            state= "afterIntra"+localSearchIterator;

                        }


                    }
                    else
                    {
                        //if no cost improving relocation move was found,
                        //or in other words if the current solution is a local optimum
                        //terminate the local search algorithm
                        terminationCondition = true;
                    }

                    if (!succesfulinterlocation) {
                        state = "after-intra-route" +vehicleNo;
                    }else if(succesfulinterlocation) {

                        if(interLCNo>0) {
                            state = "after-inter-route" + interLCNo;
                        }

                    }

                    // visualize relocation moves
                    drawRoutes(s, route, allNodes, state);


                    localSearchIterator = localSearchIterator + 1;
                }
            }



            if (succesfulinterlocation) {
                s.rt.add(route);
            }else{

                sAfterIntraRelocation.rt.add(route);

            }


            String afterInter = "afterInterlocation";
            String beforeInter = "beforeInterlocation";
            if(succesfulinterlocation) {

                sAfterInterRelocation.rt.add(route);
                interTC = route.cost;
                //route.cost = interTC;

                // visualize full solution after inter relocations
                drawRoutes(s, route, allNodes, "full-solution-after-inter");

            }

            // if vrp greedy solution and all intra and inter relocations have been made made -> complete = true;
            if (cntCust == customers.size()&& succesfulinterlocation){

                complete = true;

            // if vrp greedy solution and all intra relocations have been made made -> succesfulinterlocation = true;
            }else if (cntCust == customers.size()&&!succesfulinterlocation){

                noRoutes = vehicleNo;

                succesfulinterlocation = true;

                // concatenate all the route of the solution in one long route
                // example if we had 2 routes
                // route 1: 0-3-7-9-0
                // route 2: 0-4-6-8-0
                // the concatenation of those 2 would be 0-3-7-9-0-0-3-7-9-0
                // we will consider this long route as 1 route and we will use it to perform the
                // inter route relocations (in respect to the cost and load restrains)
                for (int i=0; i<sAfterIntraRelocation.rt.size(); i++) {
                    for (int j = 0; j < sAfterIntraRelocation.rt.get(i).nodes.size(); j++) {
                        allNodeSequence.add(sAfterIntraRelocation.rt.get(i).nodes.get(j));
                    }
                }
                route = new Route();
                route.nodes=allNodeSequence;
                route.cost = totalCostAfterIntra;
                route.currentrouteload = solutionLoad;
                s.totalCost = totalCostAfterIntra;

                sAfterInterRelocation.rt.add(route);
                sAfterInterRelocation.totalCost = totalCostAfterIntra;
                sAfterInterRelocation.totalLoad = totalLoadAfterIntra;

                // this is the total cost of the solution after the intra route relocations
                intraTC = route.cost;
                // visualize the solution routes after the intra route relocations
                drawRoutes(sAfterInterRelocation, route, allNodes, "full-solution-after-intra");

            // if there are still customers left to be entered in the greedy vrp solution
            // which means that the one the vehicles is full but the greedy vrp solution is not completed yet
            // create a new route which will be satisfied by another vehicle
            }else if (cntCust < customers.size()){
                totalCostAfterIntra = totalCostAfterIntra+route.cost;
                totalLoadAfterIntra = totalLoadAfterIntra+route.currentrouteload;

                // create a new route which will be satisfied by the next vehicle
                route = new Route();
                nodeSequence = route.nodes;
            }

        }

        // print the final results
        System.out.println("\n##################################");
        System.out.println("Before relocations");

        System.out.println("# VRP solution total cost: "+totalCostBRC+ " #");
        System.out.println("#   Total number of routes: "+vehicleNo+ "    #");
        System.out.println("After intra-route relocations");

        System.out.println("# Number of intra relocations: "+intraLCNo+ " #");
        System.out.println("# After intra total cost: "+ Double.toString(intraTC)+ " #  ");

        System.out.println("After inter-route relocations");
        System.out.println("# Number of inter relocations: "+interLCNo+ " #");
        System.out.println("# After inter total cost: "+ Double.toString(interTC)+ " #  ");
        System.out.println("##################################");


    }


    private static void findBestRelocationMove(RelocationMove rm, Route route, Solution s, double [][] distanceMatrix,Boolean succesfulinterlocation, Integer vehicleNo, Integer vehicleCapacity)
    {
        //This is a variable that will hold the cost of the best relocation move
        double bestMoveCost = Double.MAX_VALUE;

        // BVehicle and FVehicle stores the vehicle number which serves the B/F customer
        int BVehicle= -1;
        int FVehicle= -1;
        // existingLoad store the existing load of the vehicle in which node B will be relocated
        double existingLoad=0;
        //We will iterate through all available nodes to be relocated
        for (int relIndex = 1; relIndex < route.nodes.size() - 1; relIndex++)
        {
            //Node A is the predecessor of B
            Node A = route.nodes.get(relIndex - 1);
            //Node B is the relocated node
            Node B = route.nodes.get(relIndex);
            //Node C is the successor of B
            Node C = route.nodes.get(relIndex + 1);

            //We will iterate through all possible re-insertion positions for B
            for (int afterInd = 0; afterInd < route.nodes.size() -1; afterInd ++)
            {
                // Why do we have to write this line?
                // This line has to do with the nature of the 1-0 relocation
                // If afterInd == relIndex -> this would mean the solution remains unaffected
                // If afterInd == relIndex - 1 -> this would mean the solution remains unaffected
                if (afterInd != relIndex && afterInd != relIndex - 1)
                {
                    //Node F the node after which B is going to be reinserted
                    Node F = route.nodes.get(afterInd);
                    //Node G the successor of F
                    Node G = route.nodes.get(afterInd + 1);

                    //The arcs A-B, B-C, and F-G break
                    double costRemoved1 = distanceMatrix[A.ID][B.ID] + distanceMatrix[B.ID][C.ID];
                    double costRemoved2 = distanceMatrix[F.ID][G.ID];
                    double costRemoved = costRemoved1 + costRemoved2;

                    //The arcs A-C, F-B and B-G are created
                    double costAdded1 = distanceMatrix[A.ID][C.ID];
                    double costAdded2 = distanceMatrix[F.ID][B.ID] + distanceMatrix[B.ID][G.ID];
                    double costAdded = costAdded1 + costAdded2;

                    //This is the cost of the move, or in other words
                    //the change that this move will cause if applied to the current solution
                    double moveCost = costAdded - costRemoved;



                    if(succesfulinterlocation) {
                        for (int i = 0; i < vehicleNo-1; i++) {

                            for (int j = 0; j < ((s.rt.get(i).nodes).size()); j++) {


                                if ((s.rt.get(i).nodes.get(j).ID) == (B.ID)) {
                                    BVehicle = i;
                                }

                                if ((s.rt.get(i).nodes.get(j).ID)==(F.ID)){
                                    FVehicle= i;
                                }

                            }

                        }
                    }else if (!succesfulinterlocation){
                        BVehicle = vehicleNo-1;
                        FVehicle = vehicleNo-1;


                    }


                    //If this move is the best found so far
                    // in case of intra route relocations Behivle = Fvehicle the route load will remain the same
                    if (!succesfulinterlocation) {
                        // B.demand != 0 -> make sure that node b which will be relocated is not a depot
                        if (moveCost < bestMoveCost && B.demand != 0) {
                            //set the best cost equal to the cost of this solution
                            bestMoveCost = moveCost;

                            //store its characteristics
                            rm.positionOfRelocated = relIndex;
                            rm.positionToBeInserted = afterInd;
                            rm.moveCost = moveCost;
                        }
                    }else{
                        // in case of inter route relocations Bvehivle might not be equal to Fvehicle
                        // and therefore we need to be sure that the demand of B can be satisfied by the Fvehicle
                        existingLoad =  s.rt.get(FVehicle).currentrouteload;
                        if (moveCost < bestMoveCost && B.demand != 0 && B.demand + existingLoad<= vehicleCapacity) {
                            //set the best cost equal to the cost of this solution
                            bestMoveCost = moveCost;

                            //store its characteristics
                            rm.positionOfRelocated = relIndex;
                            rm.positionToBeInserted = afterInd;
                            rm.moveCost = moveCost;
                        }



                    }



                }
            }
        }
    }

    //This function applies the relocation move rm to solution s
    private static void applyRelocationMove(RelocationMove rm, Route route,  Solution s, double[][] distanceMatrix)
    {
        //This is the node to be relocated
        Node relocatedNode = route.nodes.get(rm.positionOfRelocated);

        //Take out the relocated node
        route.nodes.remove(rm.positionOfRelocated);

        //Reinsert the relocated node into the appropriate position
        //Where??? -> after the node that WAS (!!!!) located in the rm.positionToBeInserted of the route

        //Watch out!!!
        //If the relocated customer is reinserted backwards we have to re-insert it in (rm.positionToBeInserted + 1)
        if (rm.positionToBeInserted < rm.positionOfRelocated)
        {
            route.nodes.add(rm.positionToBeInserted + 1, relocatedNode);
        }
        ////else (if it is reinserted forward) we have to re-insert it in (rm.positionToBeInserted)
        else
        {
            route.nodes.add(rm.positionToBeInserted, relocatedNode);
        }

        // The rest of the code is just for testing purposes
        // to check if everything is OK
        double newSolutionCost = 0;
        for (int i = 0 ; i < route.nodes.size() - 1; i++)
        {
            Node A = route.nodes.get(i);
            Node B = route.nodes.get(i + 1);
            newSolutionCost = newSolutionCost + distanceMatrix[A.ID][B.ID];
        }



        //update the cost of the solution and the corresponding cost of the route object in the solution
        s.totalCost = s.totalCost + rm.moveCost;
        route.cost = route.cost + rm.moveCost;
    }

    //private static void drawRoutes(Solution s, ArrayList<Node> nodes, String fileName)
    private static void drawRoutes(Solution s, Route route, ArrayList<Node> nodes, String fileName)
    {


        int VRP_Y = 800;
        int VRP_INFO = 200;
        int X_GAP = 600;
        int margin = 30;
        int marginNode = 1;
        int XXX =  VRP_INFO + X_GAP;
        int YYY =  VRP_Y;


        BufferedImage output = new BufferedImage(XXX, YYY, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = output.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, XXX, YYY);
        g.setColor(Color.BLACK);


        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;
        for (int i = 0; i < nodes.size(); i++)
        {
            project.Node n = nodes.get(i);
            if (n.x > maxX) maxX = n.x;
            if (n.x < minX) minX = n.x;
            if (n.y > maxY) maxY = n.y;
            if (n.y < minY) minY = n.y;
        }

        int mX = XXX - 2 * margin;
        int mY = VRP_Y - 2 * margin;

        int A, B;
        if ((maxX - minX) > (maxY - minY))
        {
            A = mX;
            B = (int)((double)(A) * (maxY - minY) / (maxX - minX));
            if (B > mY)
            {
                B = mY;
                A = (int)((double)(B) * (maxX - minX) / (maxY - minY));
            }
        }
        else
        {
            B = mY;
            A = (int)((double)(B) * (maxX - minX) / (maxY - minY));
            if (A > mX)
            {
                A = mX;
                B = (int)((double)(A) * (maxY - minY) / (maxX - minX));
            }
        }

        // Draw Route
        //for (int i = 1; i < s.rt.nodes.size(); i++)
        for (int i = 1; i < route.nodes.size(); i++)

        {
            project.Node n;
            //n = s.rt.nodes.get(i - 1);
            n = route.nodes.get(i - 1);

            int ii1 = (int)((double)(A) * ((n.x - minX) / (maxX - minX) - 0.5) + (double)mX / 2) + margin;
            int jj1 = (int)((double)(B) * (0.5 - (n.y - minY) / (maxY - minY)) + (double)mY / 2) + margin;
            //n = s.rt.nodes.get(i);
            n = route.nodes.get(i);

            int ii2 = (int)((double)(A) * ((n.x - minX) / (maxX - minX) - 0.5) + (double)mX / 2) + margin;
            int jj2 = (int)((double)(B) * (0.5 - (n.y - minY) / (maxY - minY)) + (double)mY / 2) + margin;


            g.drawLine(ii1, jj1, ii2, jj2);
        }

        for (int i = 0; i < nodes.size(); i++)
        {
            project.Node n = nodes.get(i);

            int ii = (int)((double)(A) * ((n.x - minX) / (maxX - minX) - 0.5) + (double)mX / 2) + margin;
            int jj = (int)((double)(B) * (0.5 - (n.y - minY) / (maxY - minY)) + (double)mY / 2) + margin;
            if (i != 0)
            {
                g.fillOval(ii - 2 * marginNode, jj - 2 * marginNode, 4 * marginNode, 4 * marginNode);
                String id = Integer.toString(n.ID);
                g.drawString(id, ii + 8 * marginNode, jj+ 8 * marginNode);
            }
            else
            {
                g.fillRect(ii - 4 * marginNode, jj - 4 * marginNode, 8 * marginNode, 8 * marginNode);
                String id = Integer.toString(n.ID);
                g.drawString(id, ii + 8 * marginNode, jj + 8 * marginNode);
            }
        }

        //String cst = "Cost: " + s.cost;
        String cst = "Cost: " + route.cost;

        g.drawString(cst, 10, 10);

        fileName = fileName + ".png";
        File f = new File(fileName);
        try
        {
            ImageIO.write(output, "PNG", f);
        } catch (IOException ex) {
            Logger.getLogger(project.VRPComponents.class.getName()).log(Level.SEVERE, null, ex);
        }

    }


}

// this is the customer object with its id, coordinates and name
class Node
{
    int x;
    int y;
    int ID;
    int demand;

    // true/false flag indicating if a customer has been inserted in the solution
    boolean isRouted;

    Node()
    {



    }
}

// the solution object contains an arraylist of route objects, the solution total cost and the solution total load
class Solution
{

    double totalCost;
    double totalLoad;
    ArrayList <project.Route> rt;

    //This is the Solution constructor. It is executed every time a new Solution object is created (new Solution)
    Solution ()
    {
        // A new route object is created addressed by rt
        // The constructor of route is called
        rt = new ArrayList<project.Route>();

        totalCost = 0;
        totalLoad = 0;

    }
}

// the route object contains the customers in each route, the route load and the route cost
class Route
{
    ArrayList <project.Node> nodes;
    double cost;
    double currentrouteload;

    //This is the Route constructor. It is executed every time a new Route object is created (new Route)
    Route()
    {
        cost = 0;
        currentrouteload = 0;

        // A new arraylist of nodes is created
        nodes = new ArrayList<project.Node>();



    }
}

class RelocationMove
{
    int positionOfRelocated;
    int positionToBeInserted;
    double moveCost;

    RelocationMove()
    {
    }
}

