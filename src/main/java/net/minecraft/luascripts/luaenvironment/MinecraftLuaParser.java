package net.minecraft.luascripts.luaenvironment;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.Explosion;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.VarArgFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MinecraftLuaParser {

    public static LuaTable parseServer(MinecraftServer server) {
        return new ServerParser(server).getLuaTable();
    }

    public static LuaTable parseWorld(World world) {
        return new WorldParser(world).getLuaTable();
    }

    public static LuaTable parseEntity(Entity entity) {
        return new EntityParser(entity).getLuaTable();
    }

    public static LuaTable parseBlockPos(BlockPos blockPos) {
        LuaTable table = new LuaTable();

        table.set("x", blockPos.getX());
        table.set("y", blockPos.getY());
        table.set("z", blockPos.getZ());

        return table;
    }

    protected static class ServerParser {
        MinecraftServer server;
        public ServerParser(MinecraftServer server) {
            this.server = server;
        }

        public LuaTable getLuaTable() {
            LuaTable table = new LuaTable();

            addGetWorld(table);

            return table;
        }

        private void addGetWorld(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) throws LuaError {
                        int index = ClassParser.parseInt(new Argument(varargs.arg(1), 1));
                        Iterable<ServerWorld> worldsIterable = server.getWorlds();

                        List<ServerWorld> worldsList =
                                StreamSupport.stream(worldsIterable.spliterator(), false)
                                        .collect(Collectors.toList());

                        if (index >= 0 && index < worldsList.size()) {
                            return MinecraftLuaParser.parseWorld(worldsList.get(index));
                        } else {
                            return LuaValue.NIL;
                        }
                    }
                };
                table.set("getWorld", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected static class WorldParser {
        World world;
        public WorldParser(World world) {
            this.world = world;
        }

        public LuaTable getLuaTable() {
            LuaTable table = new LuaTable();

            addExecuteCommand(table);
            addCreateExplosion(table);
            addCanBlockSeeSky(table);
            addGetSpawnPosition(table);
            addGetCurrentTime(table);
            addGetWorldID(table);
            addGetGameRuleValue(table);
            addGetMoonPhaseFactor(table);
            addGetRedstonePower(table);
            addGetRedstonePowerFromNeighbors(table);
            addIsBlockPowered(table);
            addGetLight(table);
            addIsBlockAir(table);
            addIsDaytime(table);
            addGetDifficulty(table);
            addIsRaining(table);
            addIsThundering(table);
            addGetPlayersCountOnServer(table);
            addGetPlayersCountInWorld(table);
            addGetPlayers(table);
            addGetEntitiesInSquare(table);
            addGetPlayersInSquare(table);

            return table;
        }

        private void addExecuteCommand(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) throws LuaError {
                        String command = ClassParser.parseString(new Argument(varargs.arg(1), 1));
                        BlockPos blockPos = ClassParser.parseBlockPos(new Argument(varargs.arg(2), 2, new BlockPos(0, 0, 0)));

                        try {
                            world.getServer().getCommandManager().handleCommand(
                                    new CommandSource(
                                            ICommandSource.DUMMY,
                                            new Vector3d(blockPos.getX(), blockPos.getY(), blockPos.getZ()),
                                            Vector2f.ZERO,
                                            (ServerWorld) world,
                                            4,
                                            "$\"LuaScriptExecutor\"",
                                            new StringTextComponent("$\"LuaScriptExecutor\""),
                                            world.getServer(),
                                            null
                                    ),
                                    command
                            );
                        } catch (NullPointerException e) {
                            throw new LuaError(e);
                        }

                        return LuaValue.NIL;
                    }
                };
                table.set("executeCommand", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addCreateExplosion(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        BlockPos blockPos = ClassParser.parseBlockPos(new Argument(varargs.arg(1), 1));
                        float strength = ClassParser.parseFloat(new Argument(varargs.arg(2), 2, 4));
                        String mode = ClassParser.parseString(new Argument(varargs.arg(3), 3, "break"));
                        switch (mode) {
                            case "break":
                                world.createExplosion(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), strength, Explosion.Mode.BREAK);
                                break;
                            case "destroy":
                                world.createExplosion(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), strength, Explosion.Mode.DESTROY);
                                break;
                            case "none":
                                world.createExplosion(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), strength, Explosion.Mode.NONE);
                                break;
                            default:
                                throw new LuaError("value of argument #3 is '"+mode+"', while only 'break', 'destroy' and 'none' are allowed");
                        }
                        return NIL;
                    }
                };
                table.set("createExplosion", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addCanBlockSeeSky(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        BlockPos blockPos = ClassParser.parseBlockPos(new Argument(varargs.arg(1), 1));
                        return LuaValue.valueOf(world.canBlockSeeSky(blockPos));
                    }
                };
                table.set("canBlockSeeSky", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addGetSpawnPosition(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        return parseBlockPos(new BlockPos(world.getWorldInfo().getSpawnX(), world.getWorldInfo().getSpawnY(), world.getWorldInfo().getSpawnZ()));
                    }
                };
                table.set("getSpawnPosition", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addGetCurrentTime(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        return LuaValue.valueOf(world.getWorldInfo().getDayTime());
                    }
                };
                table.set("getCurrentTime", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addGetWorldID(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        try {
                            Iterable<ServerWorld> worldsIterable = world.getServer().getWorlds();
                            AtomicInteger index = new AtomicInteger();
                            worldsIterable.forEach(worldIterable -> {
                                if (world.getDimensionKey().equals(worldIterable.getDimensionKey()))
                                    return;
                                else
                                    index.getAndIncrement();
                            });
                            return LuaValue.valueOf(index.get());

                        } catch (NullPointerException ignored) {}
                        return LuaValue.NIL;
                    }
                };
                table.set("getWorldID", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } //TODO: Fix wrong indexes

        private void addGetGameRuleValue(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        final String gameRule = ClassParser.parseString(new Argument(varargs.arg(1), 1));

                        GameRules.RuleKey[] ruleKeyHolder = new GameRules.RuleKey[1];

                        GameRules.visitAll(new GameRules.IRuleEntryVisitor() {
                            @Override
                            public <T extends GameRules.RuleValue<T>> void visit(GameRules.RuleKey<T> key, GameRules.RuleType<T> type) {
                                if (key.getName().equals(gameRule)) {
                                    ruleKeyHolder[0] = key;
                                }
                            }
                        });

                        GameRules.RuleKey ruleKey = ruleKeyHolder[0];

                        GameRules.RuleValue ruleValue = world.getWorldInfo().getGameRulesInstance().get(ruleKey);

                        String value = ruleValue.stringValue();

                        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                            return LuaValue.valueOf(Boolean.parseBoolean(value));
                        } else if (!value.isEmpty()) {
                            try {
                                return LuaValue.valueOf(Integer.parseInt(value));
                            } catch (NumberFormatException e) {
                                return LuaValue.valueOf(value);
                            }
                        } else {
                            return LuaValue.NIL;
                        }
                    }
                };
                table.set("getGameRuleValue", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addGetMoonPhaseFactor(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        return LuaValue.valueOf(world.getDimensionType().getMoonPhase(world.getWorldInfo().getDayTime()));
                    }
                };
                table.set("getMoonPhaseFactor", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addGetRedstonePower(LuaTable table) { //TODO: Directions does not work
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        BlockPos blockPos = ClassParser.parseBlockPos(new Argument(varargs.arg(1), 1));
                        if (!varargs.arg(2).isnil()) {
                            String direction = ClassParser.parseString(new Argument(varargs.arg(2), 2));
                            switch (direction) {
                                case "up":
                                    return LuaValue.valueOf(world.getRedstonePower(blockPos, Direction.UP));
                                case "down":
                                    return LuaValue.valueOf(world.getRedstonePower(blockPos, Direction.DOWN));
                                case "north":
                                    return LuaValue.valueOf(world.getRedstonePower(blockPos, Direction.NORTH));
                                case "south":
                                    return LuaValue.valueOf(world.getRedstonePower(blockPos, Direction.SOUTH));
                                case "east":
                                    return LuaValue.valueOf(world.getRedstonePower(blockPos, Direction.EAST));
                                case "west":
                                    return LuaValue.valueOf(world.getRedstonePower(blockPos, Direction.WEST));
                                default:
                                    throw new LuaError("value of argument #2 is '"+direction+"', while only 'up', 'down', 'north', 'south', 'east' and 'west' are allowed");
                            }
                        } else {
                            return LuaValue.valueOf(
                                    Math.max(
                                            Math.max(world.getRedstonePower(blockPos, Direction.UP), world.getRedstonePower(blockPos, Direction.DOWN)),
                                            Math.max(
                                                    Math.max(world.getRedstonePower(blockPos, Direction.NORTH), world.getRedstonePower(blockPos, Direction.SOUTH)),
                                                    Math.max(world.getRedstonePower(blockPos, Direction.EAST), world.getRedstonePower(blockPos, Direction.WEST))
                                            )
                                    )
                            );
                        }
                    }
                };
                table.set("getRedstonePower", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addGetRedstonePowerFromNeighbors(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        BlockPos blockPos = ClassParser.parseBlockPos(new Argument(varargs.arg(1), 1));
                        return valueOf(world.getRedstonePowerFromNeighbors(blockPos));
                    }
                };
                table.set("getRedstonePowerFromNeighbors", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addIsBlockPowered(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        BlockPos blockPos = ClassParser.parseBlockPos(new Argument(varargs.arg(1), 1));
                        return LuaValue.valueOf(world.isBlockPowered(blockPos));
                    }
                };
                table.set("isBlockPowered", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addGetLight(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        BlockPos blockPos = ClassParser.parseBlockPos(new Argument(varargs.arg(1), 1));
                        return LuaValue.valueOf(world.getLight(blockPos));
                    }
                };
                table.set("getLight", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addIsBlockAir(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        BlockPos blockPos = ClassParser.parseBlockPos(new Argument(varargs.arg(1), 1));
                        return LuaValue.valueOf(world.isAirBlock(blockPos));
                    }
                };
                table.set("isBlockAir", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addIsDaytime(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        return LuaValue.valueOf(world.isDaytime());
                    }
                };
                table.set("isDaytime", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addGetDifficulty(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        return LuaValue.valueOf(world.getDifficulty().getDisplayName().getString());
                    }
                };
                table.set("getDifficulty", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addIsRaining(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        return LuaValue.valueOf(world.isRaining());
                    }
                };
                table.set("isRaining", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addIsThundering(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        return LuaValue.valueOf(world.isThundering());
                    }
                };
                table.set("isThundering", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addGetPlayersCountOnServer(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        try {
                            return LuaValue.valueOf(world.isRemote()
                                    ? Objects.requireNonNull(Minecraft.getInstance().getConnection()).getPlayerInfoMap().size()
                                    : ServerLifecycleHooks.getCurrentServer().getCurrentPlayerCount());
                        } catch (NullPointerException e) {
                            return LuaValue.NIL;
                        }
                    }
                };
                table.set("getPlayersCountOnServer", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addGetPlayersCountInWorld(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        return LuaValue.valueOf(world.getPlayers().size());
                    }
                };
                table.set("getPlayersCountInWorld", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addGetPlayers(LuaTable table) { //TODO: Empty
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        LuaTable playersLuaTableList = new LuaTable();

                        int index = 1;

                        for (PlayerEntity player: world.getPlayers()) {
                            PlayerParser playerParser = new PlayerParser(player);
                            playersLuaTableList.set(index, playerParser.getLuaTable());
                            index++;
                        }

                        return playersLuaTableList;
                    }
                };
                table.set("getPlayers", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addGetEntitiesInSquare(LuaTable table) { //TODO: Empty
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        BlockPos blockPos1 = ClassParser.parseBlockPos(new Argument(varargs.arg(1), 1));
                        BlockPos blockPos2 = ClassParser.parseBlockPos(new Argument(varargs.arg(2), 2));

                        LuaTable entitiesLuaTableList = new LuaTable();

                        int index = 1;


                        for (Entity entity: world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(blockPos1, blockPos2))) {
                            EntityParser entityParser = new EntityParser(entity);
                            entitiesLuaTableList.set(index, entityParser.getLuaTable());
                            index++;
                        }

                        return entitiesLuaTableList;
                    }
                };
                table.set("getEntitiesInSquare", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addGetPlayersInSquare(LuaTable table) { //TODO: Empty
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        BlockPos blockPos1 = ClassParser.parseBlockPos(new Argument(varargs.arg(1), 1));
                        BlockPos blockPos2 = ClassParser.parseBlockPos(new Argument(varargs.arg(2), 2));

                        LuaTable entitiesLuaTableList = new LuaTable();

                        int index = 1;

                        for (Entity player: world.getEntitiesWithinAABB(PlayerEntity.class, new AxisAlignedBB(blockPos1, blockPos2))) {
                            EntityParser entityParser = new EntityParser(player);
                            entitiesLuaTableList.set(index, entityParser.getLuaTable());
                            index++;
                        }

                        return entitiesLuaTableList;
                    }
                };
                table.set("getPlayersInSquare", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected static class EntityParser {
        Entity entity;
        public EntityParser(Entity entity) {
            this.entity = entity;
        }

        public LuaTable getLuaTable() {
            LuaTable table = new LuaTable();

            addGetDisplayName(table);
            addGetId(table);
            addGetPosition(table);
            addGetLookingBlockPosition(table);

            return table;
        }

        private void addGetDisplayName(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        return LuaValue.valueOf(entity.getDisplayName().getString());
                    }
                };
                table.set("getDisplayName", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addGetId(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        return LuaValue.valueOf(entity.getUniqueID().toString());
                    }
                };
                table.set("getId", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addGetPosition(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        LuaTable position = new LuaTable();
                        position.set("x", entity.getPosX());
                        position.set("y", entity.getPosY());
                        position.set("z", entity.getPosZ());
                        return position;
                    }
                };
                table.set("getPosition", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addGetLookingBlockPosition(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        LuaTable position = new LuaTable();
                        int distance = ClassParser.parseInt(new Argument(varargs.arg(1), 1, 512));
                        position.set("x", entity.world.rayTraceBlocks(new RayTraceContext(entity.getEyePosition(1f),
                                entity.getEyePosition(1f).add(entity.getLook(1f).x * distance, entity.getLook(1f).y * distance, entity.getLook(1f).z * distance),
                                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, entity)).getPos().getX());
                        position.set("y", entity.world.rayTraceBlocks(new RayTraceContext(entity.getEyePosition(1f),
                                entity.getEyePosition(1f).add(entity.getLook(1f).x * distance, entity.getLook(1f).y * distance, entity.getLook(1f).z * distance),
                                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, entity)).getPos().getY());
                        position.set("z", entity.world.rayTraceBlocks(new RayTraceContext(entity.getEyePosition(1f),
                                entity.getEyePosition(1f).add(entity.getLook(1f).x * distance, entity.getLook(1f).y * distance, entity.getLook(1f).z * distance),
                                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, entity)).getPos().getZ());

                        return position;
                    }
                };
                table.set("getLookingBlockPosition", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected static class PlayerParser {
        PlayerEntity player;
        public PlayerParser(PlayerEntity playerEntity) {
            this.player = playerEntity;
        }

        public LuaTable getLuaTable() {
            LuaTable table = new LuaTable();

            addGetDisplayName(table);
            addGetId(table);
            addGetPosition(table);
            addGetLookingBlockPosition(table);

            return table;
        }

        private void addGetDisplayName(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        return LuaValue.valueOf(player.getDisplayName().getString());
                    }
                };
                table.set("getDisplayName", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addGetId(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        return LuaValue.valueOf(player.getUniqueID().toString());
                    }
                };
                table.set("getId", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addGetPosition(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        LuaTable position = new LuaTable();
                        position.set("x", player.getPosX());
                        position.set("y", player.getPosY());
                        position.set("z", player.getPosZ());
                        return position;
                    }
                };
                table.set("getPosition", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void addGetLookingBlockPosition(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {
                        LuaTable position = new LuaTable();
                        int distance = ClassParser.parseInt(new Argument(varargs.arg(1), 1, 512));
                        position.set("x", player.world.rayTraceBlocks(new RayTraceContext(player.getEyePosition(1f),
                                player.getEyePosition(1f).add(player.getLook(1f).x * distance, player.getLook(1f).y * distance, player.getLook(1f).z * distance),
                                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, player)).getPos().getX());
                        position.set("y", player.world.rayTraceBlocks(new RayTraceContext(player.getEyePosition(1f),
                                player.getEyePosition(1f).add(player.getLook(1f).x * distance, player.getLook(1f).y * distance, player.getLook(1f).z * distance),
                                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, player)).getPos().getY());
                        position.set("z", player.world.rayTraceBlocks(new RayTraceContext(player.getEyePosition(1f),
                                player.getEyePosition(1f).add(player.getLook(1f).x * distance, player.getLook(1f).y * distance, player.getLook(1f).z * distance),
                                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, player)).getPos().getZ());

                        return position;
                    }
                };
                table.set("getLookingBlockPosition", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*  private void add(LuaTable table) {
            try {
                LuaFunction function = new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs varargs) {

                        return LuaValue.NIL;
                    }
                };
                table.set("", function);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/

    /**/
}
