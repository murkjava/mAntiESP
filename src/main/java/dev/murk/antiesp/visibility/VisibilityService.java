package dev.murk.antiesp.visibility;

import dev.murk.antiesp.cache.ChunkCacheManager;
import dev.murk.antiesp.config.Config;
import dev.murk.antiesp.raytrace.FastRaytracer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public record VisibilityService(ChunkCacheManager cacheManager, Config config,
                                Map<UUID, Location> lastLocations,
                                Map<UUID, Vector> velocities) {
    public VisibilityService(ChunkCacheManager cacheManager, Config config) {
        this(cacheManager, config, new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }

    public VisibilityService(ChunkCacheManager cacheManager) {
        this(cacheManager, new Config());
    }

    public boolean canSee(Player observer, Entity target) {
        if (observer == null || target == null) return false;

        if (observer.hasPermission("mantiesp.bypass")) return true;

        if (config.getHide().isIgnoreSpectator() && observer.getGameMode() == GameMode.SPECTATOR) return true;

        World world = observer.getWorld();
        if (!world.equals(target.getWorld())) return false;

        if (config.isWorldDisabled(world.getName())) return true;

        if (!config.shouldCheckEntity(target)) return true;

        Location eye = observer.getEyeLocation();
        Location targetLoc = target.getLocation();

        double dx = targetLoc.getX() - eye.getX();
        double dy = targetLoc.getY() - eye.getY();
        double dz = targetLoc.getZ() - eye.getZ();
        double distSq = dx * dx + dy * dy + dz * dz;

        if (distSq <= 0.04) {
            return true;
        }

        double maxDist = config.getMaxDistance() + (config.getF5().isEnabled() ? config.getF5().getDistance() : 0.0);
        if (distSq > maxDist * maxDist) return false;

        if (!config.getHide().isIgnoreGlowing() && isGlowing(target)) return true;

        if (config.getHide().isBlindness() && observer.hasPotionEffect(PotionEffectType.BLINDNESS)) {
            if (distSq > config.getHide().getBlindnessDistanceSquared()) {
                return false;
            }
        }

        if (config.getHide().isInLava()) {
            if (eye.getBlock().getType() == Material.LAVA || observer.getLocation().getBlock().getType() == Material.LAVA) {
                if (distSq >= config.getHide().getLavaDistanceSquared()) {
                    return false;
                }
            }
        }

        double height = target.getHeight();
        double width = target.getWidth();
        double targetX = targetLoc.getX();
        double targetY = targetLoc.getY();
        double targetZ = targetLoc.getZ();

        double startX = eye.getX();
        double startY = eye.getY();
        double startZ = eye.getZ();
        UUID worldId = world.getUID();

        if (distSq <= config.getMinDistanceSquared()) {
            if (!(target instanceof Player player && player.isSneaking())) {
                if (FastRaytracer.canSee(cacheManager, worldId, startX, startY, startZ, targetX, targetY + (height * 0.5), targetZ)) {
                    return true;
                }
            }
        }

        if (checkVisibility(worldId, startX, startY, startZ, targetX, targetY, targetZ, width, height)) {
            return true;
        }

        if (config.getF5().isEnabled()) {
            Vector look = eye.getDirection();
            double lookX = look.getX();
            double lookY = look.getY();
            double lookZ = look.getZ();
            double f5Dist = config.getF5().getDistance();
            double f5Offset = config.getF5().getCollisionOffset();

            Vector camBack = FastRaytracer.clipCamera(cacheManager, worldId, startX, startY, startZ, -lookX, -lookY, -lookZ, f5Dist, f5Offset);
            if (camBack != null && checkVisibility(worldId, camBack.getX(), camBack.getY(), camBack.getZ(), targetX, targetY, targetZ, width, height)) {
                return true;
            }

            if (config.getF5().isFrontView()) {
                Vector camFront = FastRaytracer.clipCamera(cacheManager, worldId, startX, startY, startZ, lookX, lookY, lookZ, f5Dist, f5Offset);
                if (camFront != null && checkVisibility(worldId, camFront.getX(), camFront.getY(), camFront.getZ(), targetX, targetY, targetZ, width, height)) {
                    return true;
                }
            }
        }

        if (config.isPredictMovement() && config.getPredictionMultiplier() > 0.0) {
            double mult = config.getPredictionMultiplier();
            Vector targetVel = getVelocity(target);
            Vector obsVel = getVelocity(observer);

            boolean targetMoving = (targetVel.getX() * targetVel.getX() + targetVel.getZ() * targetVel.getZ() > 0.001) || Math.abs(targetVel.getY()) > 0.1;
            boolean obsMoving = (obsVel.getX() * obsVel.getX() + obsVel.getZ() * obsVel.getZ() > 0.001) || Math.abs(obsVel.getY()) > 0.1;

            if (targetMoving || obsMoving) {
                double predStartX = startX + (obsMoving ? obsVel.getX() * mult : 0.0);
                double predStartY = startY + (obsMoving ? obsVel.getY() * mult : 0.0);
                double predStartZ = startZ + (obsMoving ? obsVel.getZ() * mult : 0.0);

                if (obsMoving && !FastRaytracer.canSee(cacheManager, worldId, startX, startY, startZ, predStartX, predStartY, predStartZ)) {
                    predStartX = startX;
                    predStartY = startY;
                    predStartZ = startZ;
                }

                double predTargetX = targetX + (targetMoving ? targetVel.getX() * mult : 0.0);
                double predTargetY = targetY + (targetMoving ? targetVel.getY() * mult : 0.0);
                double predTargetZ = targetZ + (targetMoving ? targetVel.getZ() * mult : 0.0);

                if (targetMoving && !FastRaytracer.canSee(cacheManager, worldId, targetX, targetY, targetZ, predTargetX, predTargetY, predTargetZ)) {
                    predTargetX = targetX;
                    predTargetY = targetY;
                    predTargetZ = targetZ;
                }

                if (checkVisibility(worldId, predStartX, predStartY, predStartZ, predTargetX, predTargetY, predTargetZ, width, height)) {
                    return true;
                }

                if (targetMoving && obsMoving) {
                    if (checkVisibility(worldId, startX, startY, startZ, predTargetX, predTargetY, predTargetZ, width, height)) {
                        return true;
                    }
                }

                if (config.getF5().isEnabled()) {
                    Vector look = eye.getDirection();
                    double lookX = look.getX();
                    double lookY = look.getY();
                    double lookZ = look.getZ();
                    double f5Dist = config.getF5().getDistance();
                    double f5Offset = config.getF5().getCollisionOffset();

                    Vector camBack = FastRaytracer.clipCamera(cacheManager, worldId, predStartX, predStartY, predStartZ, -lookX, -lookY, -lookZ, f5Dist, f5Offset);
                    if (camBack != null && checkVisibility(worldId, camBack.getX(), camBack.getY(), camBack.getZ(), predTargetX, predTargetY, predTargetZ, width, height)) {
                        return true;
                    }

                    if (config.getF5().isFrontView()) {
                        Vector camFront = FastRaytracer.clipCamera(cacheManager, worldId, predStartX, predStartY, predStartZ, lookX, lookY, lookZ, f5Dist, f5Offset);
                        if (camFront != null && checkVisibility(worldId, camFront.getX(), camFront.getY(), camFront.getZ(), predTargetX, predTargetY, predTargetZ, width, height)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public boolean canSee(Location start, Location end) {
        if (start == null || end == null) {
            return false;
        }

        World world = start.getWorld();
        if (world == null || !world.equals(end.getWorld())) {
            return false;
        }

        if (config.isWorldDisabled(world.getName())) {
            return true;
        }

        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double dz = end.getZ() - start.getZ();
        double distSq = dx * dx + dy * dy + dz * dz;

        if (distSq <= config.getMinDistanceSquared()) {
            return true;
        }

        if (distSq > config.getMaxDistanceSquared()) {
            return false;
        }

        return FastRaytracer.canSee(cacheManager, world.getUID(),
                start.getX(), start.getY(), start.getZ(),
                end.getX(), end.getY(), end.getZ());
    }

    public boolean canSee(Player observer, double targetX, double targetY, double targetZ, double height) {
        if (observer == null) {
            return false;
        }

        World world = observer.getWorld();
        if (config.isWorldDisabled(world.getName())) {
            return true;
        }

        Location eye = observer.getEyeLocation();
        double dx = targetX - eye.getX();
        double dy = targetY - eye.getY();
        double dz = targetZ - eye.getZ();
        double distSq = dx * dx + dy * dy + dz * dz;

        if (distSq <= 0.04) {
            return true;
        }

        double maxDist = config.getMaxDistance() + (config.getF5().isEnabled() ? config.getF5().getDistance() : 0.0);
        if (distSq > maxDist * maxDist) {
            return false;
        }

        if (config.getHide().isBlindness() && observer.hasPotionEffect(PotionEffectType.BLINDNESS)) {
            if (distSq > config.getHide().getBlindnessDistanceSquared()) {
                return false;
            }
        }

        if (config.getHide().isInLava()) {
            if (eye.getBlock().getType() == Material.LAVA || observer.getLocation().getBlock().getType() == Material.LAVA) {
                if (distSq >= config.getHide().getLavaDistanceSquared()) {
                    return false;
                }
            }
        }

        double startX = eye.getX();
        double startY = eye.getY();
        double startZ = eye.getZ();
        UUID worldId = world.getUID();

        if (distSq <= config.getMinDistanceSquared()) {
            if (FastRaytracer.canSee(cacheManager, worldId, startX, startY, startZ, targetX, targetY + (height * 0.5), targetZ)) {
                return true;
            }
        }

        if (checkVisibility(worldId, startX, startY, startZ, targetX, targetY, targetZ, 0.6, height)) {
            return true;
        }

        if (config.getF5().isEnabled()) {
            Vector look = eye.getDirection();
            double lookX = look.getX();
            double lookY = look.getY();
            double lookZ = look.getZ();
            double f5Dist = config.getF5().getDistance();
            double f5Offset = config.getF5().getCollisionOffset();

            Vector camBack = FastRaytracer.clipCamera(cacheManager, worldId, startX, startY, startZ, -lookX, -lookY, -lookZ, f5Dist, f5Offset);
            if (camBack != null && checkVisibility(worldId, camBack.getX(), camBack.getY(), camBack.getZ(), targetX, targetY, targetZ, 0.6, height)) {
                return true;
            }

            if (config.getF5().isFrontView()) {
                Vector camFront = FastRaytracer.clipCamera(cacheManager, worldId, startX, startY, startZ, lookX, lookY, lookZ, f5Dist, f5Offset);
                if (camFront != null && checkVisibility(worldId, camFront.getX(), camFront.getY(), camFront.getZ(), targetX, targetY, targetZ, 0.6, height)) {
                    return true;
                }
            }
        }

        if (config.isPredictMovement() && config.getPredictionMultiplier() > 0.0) {
            double mult = config.getPredictionMultiplier();
            Vector obsVel = getVelocity(observer);
            boolean obsMoving = (obsVel.getX() * obsVel.getX() + obsVel.getZ() * obsVel.getZ() > 0.001) || Math.abs(obsVel.getY()) > 0.1;
            if (obsMoving) {
                double predStartX = startX + obsVel.getX() * mult;
                double predStartY = startY + obsVel.getY() * mult;
                double predStartZ = startZ + obsVel.getZ() * mult;

                if (!FastRaytracer.canSee(cacheManager, worldId, startX, startY, startZ, predStartX, predStartY, predStartZ)) {
                    predStartX = startX;
                    predStartY = startY;
                    predStartZ = startZ;
                }

                if (checkVisibility(worldId, predStartX, predStartY, predStartZ, targetX, targetY, targetZ, 0.6, height)) {
                    return true;
                }

                if (config.getF5().isEnabled()) {
                    Vector look = eye.getDirection();
                    double lookX = look.getX();
                    double lookY = look.getY();
                    double lookZ = look.getZ();
                    double f5Dist = config.getF5().getDistance();
                    double f5Offset = config.getF5().getCollisionOffset();

                    Vector camBack = FastRaytracer.clipCamera(cacheManager, worldId, predStartX, predStartY, predStartZ, -lookX, -lookY, -lookZ, f5Dist, f5Offset);
                    if (camBack != null && checkVisibility(worldId, camBack.getX(), camBack.getY(), camBack.getZ(), targetX, targetY, targetZ, 0.6, height)) {
                        return true;
                    }

                    if (config.getF5().isFrontView()) {
                        Vector camFront = FastRaytracer.clipCamera(cacheManager, worldId, predStartX, predStartY, predStartZ, lookX, lookY, lookZ, f5Dist, f5Offset);
                        if (camFront != null && checkVisibility(worldId, camFront.getX(), camFront.getY(), camFront.getZ(), targetX, targetY, targetZ, 0.6, height)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private boolean checkVisibility(UUID worldId,
                                    double startX, double startY, double startZ,
                                    double targetX, double targetY, double targetZ,
                                    double width, double height) {
        double expX = config.getHitboxExpansionX();
        double expY = config.getHitboxExpansionY();
        double expZ = config.getHitboxExpansionZ();

        double midY = targetY + (height * 0.5);
        double topY = targetY + (height * 0.85) + expY;
        double bottomY = targetY + 0.1 - expY;

        if (FastRaytracer.canSee(cacheManager, worldId, startX, startY, startZ, targetX, midY, targetZ)) {
            return true;
        }

        if (FastRaytracer.canSee(cacheManager, worldId, startX, startY, startZ, targetX, topY, targetZ)) {
            return true;
        }

        double dirX = targetX - startX;
        double dirZ = targetZ - startZ;
        double horizDistSq = dirX * dirX + dirZ * dirZ;

        if (horizDistSq > 0.0001) {
            double invDist = 1.0 / Math.sqrt(horizDistSq);
            double uX = dirX * invDist;
            double uZ = dirZ * invDist;

            double nX = -uZ;
            double nZ = uX;

            double radius = (width * 0.5) + Math.max(expX, expZ);
            double offX = nX * radius;
            double offZ = nZ * radius;
            double frontX = -uX * radius;
            double frontZ = -uZ * radius;

            if (FastRaytracer.canSee(cacheManager, worldId, startX, startY, startZ, targetX + offX, midY, targetZ + offZ)) {
                return true;
            }

            if (FastRaytracer.canSee(cacheManager, worldId, startX, startY, startZ, targetX - offX, midY, targetZ - offZ)) {
                return true;
            }

            if (FastRaytracer.canSee(cacheManager, worldId, startX, startY, startZ, targetX + offX, topY, targetZ + offZ)) {
                return true;
            }

            if (FastRaytracer.canSee(cacheManager, worldId, startX, startY, startZ, targetX - offX, topY, targetZ - offZ)) {
                return true;
            }

            if (FastRaytracer.canSee(cacheManager, worldId, startX, startY, startZ, targetX + frontX, midY, targetZ + frontZ)) {
                return true;
            }

            if (FastRaytracer.canSee(cacheManager, worldId, startX, startY, startZ, targetX + frontX, topY, targetZ + frontZ)) {
                return true;
            }
        }

        return FastRaytracer.canSee(cacheManager, worldId, startX, startY, startZ, targetX, bottomY, targetZ);
    }

    public void updateLocations() {
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            Location current = player.getLocation();
            Location last = lastLocations.put(player.getUniqueId(), current);
            if (last != null && last.getWorld() != null && last.getWorld().equals(current.getWorld())) {
                double dx = current.getX() - last.getX();
                double dy = current.getY() - last.getY();
                double dz = current.getZ() - last.getZ();
                double distSq = dx * dx + dy * dy + dz * dz;
                if (distSq > 0.0001 && distSq < 100.0) {
                    double invTicks = 1.0 / Math.max(1, config.getTicksPeriod());
                    velocities.put(player.getUniqueId(), new Vector(dx * invTicks, dy * invTicks, dz * invTicks));
                    continue;
                }
            }
            Vector fallback = player.getVelocity();
            velocities.put(player.getUniqueId(), fallback);
        }
    }

    public void removePlayer(UUID uuid) {
        if (uuid != null) {
            lastLocations.remove(uuid);
            velocities.remove(uuid);
        }
    }

    public Vector getVelocity(Entity entity) {
        if (entity == null) {
            return new Vector(0, 0, 0);
        }
        Vector vel = velocities.get(entity.getUniqueId());
        if (vel != null) {
            return vel;
        }
        return entity.getVelocity();
    }

    public boolean isGlowing(Entity target) {
        if (target == null) {
            return false;
        }
        if (target.isGlowing()) {
            return true;
        }
        if (target instanceof LivingEntity living) {
            return living.hasPotionEffect(PotionEffectType.GLOWING);
        }
        return false;
    }

    public boolean canSeeNametag(Player observer, Entity target) {
        if (!hasVisibleNametag(target)) return false;

        Location eye = observer.getEyeLocation();
        Location targetLoc = target.getLocation();
        double dx = targetLoc.getX() - eye.getX();
        double dy = targetLoc.getY() - eye.getY();
        double dz = targetLoc.getZ() - eye.getZ();
        double distSq = dx * dx + dy * dy + dz * dz;
        double maxDist = config.getMaxDistance() + (config.getF5().isEnabled() ? config.getF5().getDistance() : 0.0);
        if (distSq > maxDist * maxDist) {
            return false;
        }

        if (config.getHide().isBlindness() && observer.hasPotionEffect(PotionEffectType.BLINDNESS)) {
            if (distSq > config.getHide().getBlindnessDistanceSquared()) {
                return false;
            }
        }

        return true;
    }

    public boolean hasVisibleNametag(Entity target) {
        if (target instanceof Player player) {
            return !player.isSneaking()
                    && !player.hasPotionEffect(PotionEffectType.INVISIBILITY)
                    && !player.isInvisible()
                    && player.getGameMode() != GameMode.SPECTATOR;
        } else if (target instanceof LivingEntity living) {
            return living.isCustomNameVisible()
                    && living.getCustomName() != null
                    && !living.hasPotionEffect(PotionEffectType.INVISIBILITY)
                    && !living.isInvisible();
        }
        return false;
    }
}
