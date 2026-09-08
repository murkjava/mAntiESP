package dev.murk.antiesp.cache;

public record BlockBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
    public static final BlockBox FULL_CUBE = new BlockBox(0, 0, 0, 1, 1, 1);
    public static final BlockBox SLAB_BOTTOM = new BlockBox(0, 0, 0, 1, 0.5f, 1);
    public static final BlockBox SLAB_TOP = new BlockBox(0, 0.5f, 0, 1, 1, 1);
    public static final BlockBox CARPET = new BlockBox(0, 0, 0, 1, 0.0625f, 1);
    public static final BlockBox BED = new BlockBox(0, 0, 0, 1, 0.5625f, 1);

    public boolean intersects(double x0, double y0, double z0,
                              double x1, double y1, double z1,
                              int blockX, int blockY, int blockZ) {
        double bx0 = blockX + minX;
        double by0 = blockY + minY;
        double bz0 = blockZ + minZ;
        double bx1 = blockX + maxX;
        double by1 = blockY + maxY;
        double bz1 = blockZ + maxZ;

        double dx = x1 - x0;
        double dy = y1 - y0;
        double dz = z1 - z0;

        double tMin = 0.0;
        double tMax = 1.0;

        if (Math.abs(dx) > 1e-9) {
            double invDx = 1.0 / dx;
            double t1 = (bx0 - x0) * invDx;
            double t2 = (bx1 - x0) * invDx;
            if (t1 > t2) {
                double temp = t1;
                t1 = t2;
                t2 = temp;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) {
                return false;
            }
        } else if (x0 < bx0 || x0 > bx1) {
            return false;
        }

        if (Math.abs(dy) > 1e-9) {
            double invDy = 1.0 / dy;
            double t1 = (by0 - y0) * invDy;
            double t2 = (by1 - y0) * invDy;
            if (t1 > t2) {
                double temp = t1;
                t1 = t2;
                t2 = temp;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) {
                return false;
            }
        } else if (y0 < by0 || y0 > by1) {
            return false;
        }

        if (Math.abs(dz) > 1e-9) {
            double invDz = 1.0 / dz;
            double t1 = (bz0 - z0) * invDz;
            double t2 = (bz1 - z0) * invDz;
            if (t1 > t2) {
                double temp = t1;
                t1 = t2;
                t2 = temp;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) {
                return false;
            }
        } else if (z0 < bz0 || z0 > bz1) {
            return false;
        }

        return tMin <= tMax;
    }

    public double clip(double x0, double y0, double z0,
                       double x1, double y1, double z1,
                       int blockX, int blockY, int blockZ) {
        double bx0 = blockX + minX;
        double by0 = blockY + minY;
        double bz0 = blockZ + minZ;
        double bx1 = blockX + maxX;
        double by1 = blockY + maxY;
        double bz1 = blockZ + maxZ;

        double dx = x1 - x0;
        double dy = y1 - y0;
        double dz = z1 - z0;

        double tMin = 0.0;
        double tMax = 1.0;

        if (Math.abs(dx) > 1e-9) {
            double invDx = 1.0 / dx;
            double t1 = (bx0 - x0) * invDx;
            double t2 = (bx1 - x0) * invDx;
            if (t1 > t2) {
                double temp = t1;
                t1 = t2;
                t2 = temp;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) {
                return -1.0;
            }
        } else if (x0 < bx0 || x0 > bx1) {
            return -1.0;
        }

        if (Math.abs(dy) > 1e-9) {
            double invDy = 1.0 / dy;
            double t1 = (by0 - y0) * invDy;
            double t2 = (by1 - y0) * invDy;
            if (t1 > t2) {
                double temp = t1;
                t1 = t2;
                t2 = temp;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) {
                return -1.0;
            }
        } else if (y0 < by0 || y0 > by1) {
            return -1.0;
        }

        if (Math.abs(dz) > 1e-9) {
            double invDz = 1.0 / dz;
            double t1 = (bz0 - z0) * invDz;
            double t2 = (bz1 - z0) * invDz;
            if (t1 > t2) {
                double temp = t1;
                t1 = t2;
                t2 = temp;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) {
                return -1.0;
            }
        } else if (z0 < bz0 || z0 > bz1) {
            return -1.0;
        }

        return (tMin <= tMax && tMin <= 1.0) ? tMin : -1.0;
    }
}
