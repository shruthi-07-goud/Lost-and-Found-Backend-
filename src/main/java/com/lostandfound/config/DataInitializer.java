package com.lostandfound.config;

import com.lostandfound.model.*;
import com.lostandfound.repository.ClaimRepository;
import com.lostandfound.repository.ItemRepository;
import com.lostandfound.repository.UserRepository;
import com.lostandfound.service.MatchingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ClaimRepository claimRepository;
    private final PasswordEncoder passwordEncoder;
    private final MatchingService matchingService;

    @Autowired
    public DataInitializer(UserRepository userRepository,
                           ItemRepository itemRepository,
                           ClaimRepository claimRepository,
                           PasswordEncoder passwordEncoder,
                           MatchingService matchingService) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.claimRepository = claimRepository;
        this.passwordEncoder = passwordEncoder;
        this.matchingService = matchingService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            logger.info("Database already contains data, skipping sample seeding.");
            return;
        }

        logger.info("Seeding initial demo data for college/hostel community...");

        // 1. Seed Demo Users
        User rohan = new User(
                "Rohan Sharma",
                "rohan@college.edu",
                passwordEncoder.encode("password123"),
                "+91 9876543210",
                Role.ROLE_USER
        );
        userRepository.save(rohan);

        User priya = new User(
                "Priya Patel",
                "priya@college.edu",
                passwordEncoder.encode("password123"),
                "+91 9123456789",
                Role.ROLE_USER
        );
        userRepository.save(priya);

        User warden = new User(
                "Hostel Warden (Admin)",
                "admin@college.edu",
                passwordEncoder.encode("adminpassword"),
                "+91 9988776655",
                Role.ROLE_ADMIN
        );
        userRepository.save(warden);

        // 2. Seed Sample Items with high matching compatibility
        // Item 1: Found Calculator by Rohan
        Item foundCalc = new Item();
        foundCalc.setType(ItemType.FOUND);
        foundCalc.setTitle("Found Casio FX-991CW Scientific Calculator");
        foundCalc.setDescription("Black Casio scientific calculator found on table #14 in quiet study section of Central Library. Has slight scratch on the battery cover.");
        foundCalc.setCategory("Electronics");
        foundCalc.setLocation("Central Library 2nd Floor");
        foundCalc.setDateReported(LocalDate.now().minusDays(1));
        foundCalc.setPhotoUrl("https://images.unsplash.com/photo-1594980596870-8aa52a78d8cd?w=600&auto=format&fit=crop&q=80");
        foundCalc.setStatus(ItemStatus.OPEN);
        foundCalc.setReportedBy(rohan);
        itemRepository.save(foundCalc);

        // Item 2: Lost Calculator by Priya (will match strongly with Item 1)
        Item lostCalc = new Item();
        lostCalc.setType(ItemType.LOST);
        lostCalc.setTitle("Lost Casio FX Scientific Calculator");
        lostCalc.setDescription("Lost my black Casio scientific calculator with scratched back battery cover. Left around table 14 in Central Library.");
        lostCalc.setCategory("Electronics");
        lostCalc.setLocation("Central Library");
        lostCalc.setDateReported(LocalDate.now().minusDays(1));
        lostCalc.setPhotoUrl("https://images.unsplash.com/photo-1611125832047-1d7ad1e8e485?w=600&auto=format&fit=crop&q=80");
        lostCalc.setStatus(ItemStatus.OPEN);
        lostCalc.setReportedBy(priya);
        itemRepository.save(lostCalc);

        // Item 3: Found Hostel Room Keys by Rohan
        Item foundKeys = new Item();
        foundKeys.setType(ItemType.FOUND);
        foundKeys.setTitle("Found Hostel Room Keys with Marvel Keychain");
        foundKeys.setDescription("Pair of brass keys attached to a red Marvel Spider-Man keychain found near water dispenser in the dining hall.");
        foundKeys.setCategory("Keys");
        foundKeys.setLocation("Hostel Block B Dining Hall");
        foundKeys.setDateReported(LocalDate.now().minusDays(2));
        foundKeys.setPhotoUrl("https://images.unsplash.com/photo-1582139329536-e7284fece509?w=600&auto=format&fit=crop&q=80");
        foundKeys.setStatus(ItemStatus.OPEN);
        foundKeys.setReportedBy(rohan);
        itemRepository.save(foundKeys);

        // Item 4: Lost Keys by Priya (will match strongly with Item 3)
        Item lostKeys = new Item();
        lostKeys.setType(ItemType.LOST);
        lostKeys.setTitle("Lost Room Keys on Spider-Man Keychain");
        lostKeys.setDescription("Lost two hostel room keys on a red Marvel Spider-Man keychain while having dinner in Block B dining hall.");
        lostKeys.setCategory("Keys");
        lostKeys.setLocation("Hostel Block B");
        lostKeys.setDateReported(LocalDate.now().minusDays(2));
        lostKeys.setPhotoUrl("https://images.unsplash.com/photo-1622547748225-3fc4ab24d741?w=600&auto=format&fit=crop&q=80");
        lostKeys.setStatus(ItemStatus.OPEN);
        lostKeys.setReportedBy(priya);
        itemRepository.save(lostKeys);

        // Item 5: Lost Water Bottle by Rohan
        Item lostBottle = new Item();
        lostBottle.setType(ItemType.LOST);
        lostBottle.setTitle("Lost Blue Stainless Steel Water Bottle");
        lostBottle.setDescription("750ml blue insulated stainless steel bottle with silver metallic cap, left on the bench at Badminton Court 1.");
        lostBottle.setCategory("Accessories");
        lostBottle.setLocation("Sports Complex Court 1");
        lostBottle.setDateReported(LocalDate.now().minusDays(3));
        lostBottle.setPhotoUrl("https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=600&auto=format&fit=crop&q=80");
        lostBottle.setStatus(ItemStatus.OPEN);
        lostBottle.setReportedBy(rohan);
        itemRepository.save(lostBottle);

        // Run match evaluation on initial items
        matchingService.evaluateMatchesOnItemCreation(foundCalc);
        matchingService.evaluateMatchesOnItemCreation(lostCalc);
        matchingService.evaluateMatchesOnItemCreation(foundKeys);
        matchingService.evaluateMatchesOnItemCreation(lostKeys);

        logger.info("Demo data successfully seeded with 3 users and 5 realistic matching items.");
    }
}
