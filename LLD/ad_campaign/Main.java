import java.util.*;

/*
shoe company have budget, photos/videos about product, location wanna sell to customers
it creates campaigns (google ad, insta ad, tiktok ad etc.)
ad platform: show ad, track impressions, clicks, conversion


advertiser (shoe company)
compaigns (google, insta) {budget, date range, status enum, map of map<Ads> {ad : {imp: 1, click: 2}} }
ad (actual ad with pic, video etc, cost per click/imp)
class advertiser:
    map<id : campaign>
    createCampaign()
    serveAd(request)
    recordClick(ad)
    stopCampaign(campaign)

Ad -> Campaign

*************if it gets confusing just draw the relationship tree like this***************
AdManager (the service)
  └── owns many Advertisers
        └── each owns many Campaigns
              └── each owns many Ads
              └── each owns its own budget + spend + status + per-ad stats
 */
enum CampaignStatus {ACTIVE, PAUSED, COMPLETED}
class Ad {
    private int id;
    private double costPerClick;
    private double impressionCost;
    private String content;
    public Ad(int id, double costPerClick, double impressionCost, String content) {
        this.id = id;
        this.costPerClick = costPerClick;
        this.impressionCost = impressionCost;
        this.content = content;
    }
    // getters
    public int getId() {return id;}
    public double getCostPerClick() {return costPerClick;}
    public double getImpressionCost() {return impressionCost;}
    public String getContent() {return content;}
}
interface AdStrategy {
    Ad selectAd(List<Ad> ads);
}
class RandomAd implements AdStrategy {
    Random random = new Random();
    public Ad selectAd(List<Ad> ads) {
        return ads.get(random.nextInt(ads.size()));
    }
}
class RoundRobin implements AdStrategy {
    public Ad selectAd(List<Ad> ads) {
        return null;
    }
}
class Campaign {
    private int id;
    private Map<Integer, Ad> adMap;
    private double budget;
    private CampaignStatus status;
    private Map<Integer, Map<String, Integer>> stats; // {ad id : {clicks: 2, impressions: 5}}
    private AdStrategy strategy;
    public Campaign(int id, double budget, CampaignStatus status) {
        this.id = id;
        adMap = new HashMap<>();
        this.budget = budget;
        this.status = status;
        stats = new HashMap<>();
    }
    public int getId() {return id;}
    public Map<Integer, Map<String, Integer>> getStats() { return stats; }
    public void setStrategy(AdStrategy strategy) {this.strategy = strategy;}
    public void createAd(Ad ad) {
        int id = ad.getId();
        if (adMap.containsKey(id))
            throw new IllegalArgumentException("Ad id " + id + " already exists in campaign id: " + this.id);
        adMap.put(id, ad);
    }
    public Ad serveAd() {
        if (status != CampaignStatus.ACTIVE)
            throw new IllegalStateException("Campaign is not active");
        Ad ad = strategy.selectAd(new ArrayList<>(adMap.values()));
        double impressionCost = ad.getImpressionCost();
        if (budget < impressionCost) {
            this.status = CampaignStatus.COMPLETED;
            return null;
        }
        budget -= impressionCost;
        updateAdStats(ad, "impressions");
        return ad;
    }
    private void updateAdStats(Ad ad, String statType) {
        stats.compute(ad.getId(), (k,v) -> {
            if (v == null) {
                v = new HashMap<>();
                v.put("clicks", 0);
                v.put("impressions", 0);
            }
            return v;
        });
        Map<String, Integer> adStats = stats.get(ad.getId());
        adStats.compute(statType, (k, v) -> {
            return v + 1;
        });
    }
    public void recordClick(Ad ad) {
        if (status != CampaignStatus.ACTIVE)
            throw new IllegalStateException("Campaign not active");
        if (budget < ad.getCostPerClick()) {
            status = CampaignStatus.COMPLETED;
            throw new IllegalStateException("Campaign doesn't have enough budget. marking status as COMPLETE");
        }
        budget -= ad.getCostPerClick();
        updateAdStats(ad, "clicks");
    }
}
class Advertiser {
    int id;
    Map<Integer, Campaign> campaignsMap;
    public Advertiser(int id) {
        this.id = id;
        campaignsMap = new HashMap<>();
    }
    public void addCampaign(Campaign campaign) {
        campaignsMap.put(campaign.getId(), campaign);
    }
}
class AdManager {
    Map<Integer, Advertiser> map;
    public AdManager() {
        map = new HashMap<>();
    }
    public void addAdvertiser(Advertiser advertiser) {
        map.put(advertiser.id, advertiser);
    }
    public Ad serveAd(int advertiserId, int campId) {
        return map.get(advertiserId).campaignsMap.get(campId).serveAd();
    }
    public void recordClick(int advertiserId, int campId, Ad ad) {
        map.get(advertiserId).campaignsMap.get(campId).recordClick(ad);
    }
}
public class Main {
    public static void main(String[] args) {
        // one advertiser, one campaign (budget $5), random ad selection
        Advertiser nike = new Advertiser(1);
        Campaign campaign = new Campaign(100, 5.0, CampaignStatus.ACTIVE);
        campaign.setStrategy(new RandomAd());

        Ad shoeAd = new Ad(1, 2.0, 1.0, "Nike Air Max - 50% off"); // click $2, impression $1
        campaign.createAd(shoeAd);
        nike.addCampaign(campaign);

        AdManager manager = new AdManager();
        manager.addAdvertiser(nike);

        // serve a few impressions
        for (int i = 0; i < 3; i++) {
            Ad ad = manager.serveAd(1, 100);
            System.out.println("Served: " + (ad != null ? ad.getContent() : "nothing (campaign done)"));
        }

        // record one click
        manager.recordClick(1, 100, shoeAd);
        System.out.println("Click recorded.");

        // print stats
        System.out.println("Stats: " + campaign.getStats());
    }
}
