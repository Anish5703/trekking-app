package com.example.trekking_app.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = """
        The type of trail surface or terrain for a section of a trek route.
        Use this to display trail condition indicators or surface type badges in the UI.
        A single route can have multiple sections with different trail types.
        
        
        PAVED / CONSTRUCTED
        
        DHUNGA_CHAPEKO      -> stone-paved path, common on traditional Nepali trekking
                               routes. Flat stones laid side by side on the trail.
        CONCRETE_STAIRS     -> concrete steps, usually found near villages or
                               monastery entry points
        STONE_STAIRS        -> hand-laid stone steps carved into the hillside,
                               common on steep sections of popular routes
        GRAVEL_PATH         -> compacted gravel, moderate footing, generally walkable
                               in regular trekking shoes
        
        
        NATURAL
        
        EARTHEN_FOREST_TRAIL-> dirt path through forest or woodland, can get muddy
                               and slippery after rain
        ROCKY_TRAIL         -> uneven rocky terrain with loose or fixed boulders,
                               requires careful footing
        ICE_TRAIL           -> glacial or snow-covered path, crampons or microspikes
                               are typically required
        
        
        WATER CROSSINGS
        
        WATER               -> a section that requires wading through a stream or river,
                               depth and flow vary by season
        SUSPENSION_BRIDGE   -> hanging wire bridge over a river or gorge,
                               usually crossed one person at a time
        WOODEN_BRIDGE       -> fixed wooden plank bridge, generally stable but may be
                               narrow or without railings
        
        
        OTHER
        
        OTHER               -> trail type does not fit any of the above categories,
                               check the route description for details
        """,
        enumAsRef = true
)
public enum TrailType {
    DHUNGA_CHAPEKO,
    EARTHEN_FOREST_TRAIL,
    ROCKY_TRAIL,
    STONE_STAIRS,
    CONCRETE_STAIRS,
    WATER,
    SUSPENSION_BRIDGE,
    WOODEN_BRIDGE,
    GRAVEL_PATH,
    ICE_TRAIL,
    OTHER
}
