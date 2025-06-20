package net.runelite.client.plugins.microbot.util.walker;

import java.util.List;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;

/**
     * Result class for transport route analysis, comparing direct travel vs banking route.
     * Analyzes whether it's more efficient to travel directly to a destination or
     * first visit a bank to obtain required transport items.
     */
public  class TransportRouteAnalysis {
        private final boolean directIsFaster;
        private final int directDistance;
        private final List<WorldPoint> directPath;
        private final int bankingRouteDistance;
        private final BankLocation nearestBank;
        private final WorldPoint bankLocation;
        private final List<WorldPoint> pathToBank;
        private final List<WorldPoint> pathFromBank;
        private final String analysis;
        
        public TransportRouteAnalysis(boolean directIsFaster, int directDistance, List<WorldPoint> directPath,int bankingRouteDistance, 
                                   BankLocation nearestBank, WorldPoint bankLocation,List<WorldPoint> pathToBank,
                                   List<WorldPoint> pathFromBank,String analysis) {
            this.directIsFaster = directIsFaster;
            this.directDistance = directDistance;
            this.directPath = directPath;
            this.bankingRouteDistance = bankingRouteDistance;
            this.nearestBank = nearestBank;
            this.bankLocation = bankLocation;
            this.pathToBank = pathToBank;
            this.pathFromBank = pathFromBank;
            this.analysis = analysis;
        }
        
        public boolean isDirectFaster() { return directIsFaster; }
        public int getDirectDistance() { return directDistance; }
        public int getBankingRouteDistance() { return bankingRouteDistance; }
        public BankLocation getNearestBank() { return nearestBank; }
        public WorldPoint getBankLocation() { return bankLocation; }
        public String getAnalysis() { return analysis; }
        public List<WorldPoint> getDirectPath() { return directPath; }
        public List<WorldPoint> getPathToBank() { return pathToBank; }
        public List<WorldPoint> getPathFromBank() { return pathFromBank; }
        public int getTileSavings() {
            if (directDistance == -1 || bankingRouteDistance == -1) return 0;
            return Math.abs(directDistance - bankingRouteDistance);
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("TransportRouteAnalysis {\n");
            sb.append("\tdirectIsFaster: ").append(directIsFaster).append("\n");
            sb.append("\tdirectDistance: ").append(directDistance == -1 ? "N/A" : directDistance).append(" tiles\n");
            sb.append("\tbankingRouteDistance: ").append(bankingRouteDistance == -1 ? "N/A" : bankingRouteDistance).append(" tiles\n");
            sb.append("\ttileSavings: ").append(getTileSavings()).append(" tiles\n");
            sb.append("\tnearestBank: ").append(nearestBank != null ? nearestBank.name() : "None").append("\n");
            sb.append("\tbankLocation: ").append(bankLocation != null ? bankLocation : "N/A").append("\n");
            sb.append("\tanalysis: \"").append(analysis != null ? analysis : "No analysis available").append("\"\n");
            sb.append("}");
            return sb.toString();
        }
    }
    
