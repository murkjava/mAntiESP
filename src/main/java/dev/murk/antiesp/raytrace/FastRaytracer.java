package dev.murk.antiesp.raytrace;

import dev.murk.antiesp.cache.ChunkCacheManager;
import org.bukkit.util.Vector;

import java.util.UUID;

public final class FastRaytracer {

    private FastRaytracer() {
    }

    public static boolean canSee(ChunkCacheManager cacheManager, UUID worldId,
                                 double x0, double y0, double z0,
                                 double x1, double y1, double z1) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double dz = z1 - z0;

        int currentX = fastFloor(x0);
        int currentY = fastFloor(y0);
        int currentZ = fastFloor(z0);

        int endX = fastFloor(x1);
        int endY = fastFloor(y1);
        int endZ = fastFloor(z1);

        if (currentX == endX && currentY == endY && currentZ == endZ) {
            return true;
        }

        int stepX = (dx > 0) ? 1 : ((dx < 0) ? -1 : 0);
        int stepY = (dy > 0) ? 1 : ((dy < 0) ? -1 : 0);
        int stepZ = (dz > 0) ? 1 : ((dz < 0) ? -1 : 0);

        double tMaxX;
        double tDeltaX;
        if (stepX > 0) {
            tMaxX = (currentX + 1.0 - x0) / dx;
            tDeltaX = 1.0 / dx;
        } else if (stepX < 0) {
            tMaxX = (currentX - x0) / dx;
            tDeltaX = -1.0 / dx;
        } else {
            tMaxX = Double.POSITIVE_INFINITY;
            tDeltaX = Double.POSITIVE_INFINITY;
        }

        double tMaxY;
        double tDeltaY;
        if (stepY > 0) {
            tMaxY = (currentY + 1.0 - y0) / dy;
            tDeltaY = 1.0 / dy;
        } else if (stepY < 0) {
            tMaxY = (currentY - y0) / dy;
            tDeltaY = -1.0 / dy;
        } else {
            tMaxY = Double.POSITIVE_INFINITY;
            tDeltaY = Double.POSITIVE_INFINITY;
        }

        double tMaxZ;
        double tDeltaZ;
        if (stepZ > 0) {
            tMaxZ = (currentZ + 1.0 - z0) / dz;
            tDeltaZ = 1.0 / dz;
        } else if (stepZ < 0) {
            tMaxZ = (currentZ - z0) / dz;
            tDeltaZ = -1.0 / dz;
        } else {
            tMaxZ = Double.POSITIVE_INFINITY;
            tDeltaZ = Double.POSITIVE_INFINITY;
        }

        int maxSteps = Math.abs(endX - currentX) + Math.abs(endY - currentY) + Math.abs(endZ - currentZ) + 1;

        for (int step = 0; step < maxSteps; step++) {
            if (currentX == endX && currentY == endY && currentZ == endZ) {
                return true;
            }

            if (step > 0 && cacheManager.isBlocked(worldId, currentX, currentY, currentZ, x0, y0, z0, x1, y1, z1)) {
                return false;
            }

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    currentX += stepX;
                    tMaxX += tDeltaX;
                } else {
                    currentZ += stepZ;
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    currentY += stepY;
                    tMaxY += tDeltaY;
                } else {
                    currentZ += stepZ;
                    tMaxZ += tDeltaZ;
                }
            }
        }

        return true;
    }

    public static double rayTrace(ChunkCacheManager cacheManager, UUID worldId,
                                  double x0, double y0, double z0,
                                  double x1, double y1, double z1) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double dz = z1 - z0;

        int currentX = fastFloor(x0);
        int currentY = fastFloor(y0);
        int currentZ = fastFloor(z0);

        int endX = fastFloor(x1);
        int endY = fastFloor(y1);
        int endZ = fastFloor(z1);

        int stepX = (dx > 0) ? 1 : ((dx < 0) ? -1 : 0);
        int stepY = (dy > 0) ? 1 : ((dy < 0) ? -1 : 0);
        int stepZ = (dz > 0) ? 1 : ((dz < 0) ? -1 : 0);

        double tMaxX;
        double tDeltaX;
        if (stepX > 0) {
            tMaxX = (currentX + 1.0 - x0) / dx;
            tDeltaX = 1.0 / dx;
        } else if (stepX < 0) {
            tMaxX = (currentX - x0) / dx;
            tDeltaX = -1.0 / dx;
        } else {
            tMaxX = Double.POSITIVE_INFINITY;
            tDeltaX = Double.POSITIVE_INFINITY;
        }

        double tMaxY;
        double tDeltaY;
        if (stepY > 0) {
            tMaxY = (currentY + 1.0 - y0) / dy;
            tDeltaY = 1.0 / dy;
        } else if (stepY < 0) {
            tMaxY = (currentY - y0) / dy;
            tDeltaY = -1.0 / dy;
        } else {
            tMaxY = Double.POSITIVE_INFINITY;
            tDeltaY = Double.POSITIVE_INFINITY;
        }

        double tMaxZ;
        double tDeltaZ;
        if (stepZ > 0) {
            tMaxZ = (currentZ + 1.0 - z0) / dz;
            tDeltaZ = 1.0 / dz;
        } else if (stepZ < 0) {
            tMaxZ = (currentZ - z0) / dz;
            tDeltaZ = -1.0 / dz;
        } else {
            tMaxZ = Double.POSITIVE_INFINITY;
            tDeltaZ = Double.POSITIVE_INFINITY;
        }

        int maxSteps = Math.abs(endX - currentX) + Math.abs(endY - currentY) + Math.abs(endZ - currentZ) + 1;

        for (int step = 0; step < maxSteps; step++) {
            if (step > 0) {
                double t = cacheManager.clip(worldId, currentX, currentY, currentZ, x0, y0, z0, x1, y1, z1);
                if (t >= 0.0) {
                    return t;
                }
            }

            if (currentX == endX && currentY == endY && currentZ == endZ) {
                break;
            }

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    currentX += stepX;
                    tMaxX += tDeltaX;
                } else {
                    currentZ += stepZ;
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    currentY += stepY;
                    tMaxY += tDeltaY;
                } else {
                    currentZ += stepZ;
                    tMaxZ += tDeltaZ;
                }
            }
        }

        return -1.0;
    }

    public static Vector clipCamera(ChunkCacheManager cacheManager, UUID worldId,
                                    double eyeX, double eyeY, double eyeZ,
                                    double dirX, double dirY, double dirZ,
                                    double distance, double collisionOffset) {
        if (distance <= 0.0) {
            return new Vector(eyeX, eyeY, eyeZ);
        }

        double lenSq = dirX * dirX + dirY * dirY + dirZ * dirZ;
        if (lenSq < 1e-9) {
            return new Vector(eyeX, eyeY, eyeZ);
        }

        if (Math.abs(lenSq - 1.0) > 1e-5) {
            double invLen = 1.0 / Math.sqrt(lenSq);
            dirX *= invLen;
            dirY *= invLen;
            dirZ *= invLen;
        }

        double targetX = eyeX + dirX * distance;
        double targetY = eyeY + dirY * distance;
        double targetZ = eyeZ + dirZ * distance;

        double hitT = rayTrace(cacheManager, worldId, eyeX, eyeY, eyeZ, targetX, targetY, targetZ);
        if (hitT < 0.0) {
            return new Vector(targetX, targetY, targetZ);
        }

        double hitDist = hitT * distance;
        if (hitDist < 1e-4) {
            return new Vector(eyeX, eyeY, eyeZ);
        }

        double collisionDist = Math.max(0.0, hitDist - collisionOffset);
        return new Vector(eyeX + dirX * collisionDist,
                          eyeY + dirY * collisionDist,
                          eyeZ + dirZ * collisionDist);
    }

    private static int fastFloor(double value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }
}
