package com.marinho.clarinha.core;

/** Roupinhas da personagem. */
public final class Skin {
    public String id, name;
    public boolean hasHood = true;
    public int hood, trim, inner, earC, jacket, sleeve, pants, shoeL, shoeR, tailC, dressC;
    public Integer belly, stripes;
    public String ears, tail, prop;
    public boolean glow, zip, patch, horn, mane, wings, crown, fly, rainbow, dress;
    public String[] say;

    public static final int[] RAINBOW = {0xFFFF7EB6, 0xFFFFB86B, 0xFFFFE36B, 0xFF7EE08A, 0xFF6FD0FF, 0xFFB78CFF};

    public static final Skin[] ALL = build();

    private static Skin build(String id, String name) {
        Skin s = new Skin();
        s.id = id;
        s.name = name;
        return s;
    }

    private static Skin[] build() {
        Skin a = build("clarinha", "Clarinha Gatinha");
        a.hood = 0xFF24316E; a.trim = 0xFF46EFFF; a.glow = true; a.inner = 0xFFFFB6D3; a.ears = "cat";
        a.jacket = 0xFF24316E; a.sleeve = 0xFF24316E; a.pants = 0xFF3D2C74; a.shoeL = 0xFFFF5FA8; a.shoeR = 0xFF36D2FF;
        a.prop = "star"; a.zip = true; a.say = new String[]{"Miau!", "Oi!", "Vamos brincar?", "Hihi!"};

        Skin b = build("laranja", "Gatinho Laranja");
        b.hood = 0xFFF59B3E; b.trim = 0xFFFFF0D8; b.inner = 0xFFFFC4D8; b.ears = "cat"; b.stripes = 0xFFCF6A19;
        b.jacket = 0xFFF59B3E; b.sleeve = 0xFFF59B3E; b.belly = 0xFFFFF0D8; b.pants = 0xFFFFF0D8;
        b.shoeL = 0xFFFFFFFF; b.shoeR = 0xFFFFFFFF; b.tail = "cat"; b.tailC = 0xFFF59B3E;
        b.say = new String[]{"Miau!", "Miaaau!", "Rrrr..."};

        Skin c = build("coelhinha", "Coelhinha");
        c.hood = 0xFFFFFFFF; c.trim = 0xFFFFC6DC; c.inner = 0xFFFFB3CF; c.ears = "bunny";
        c.jacket = 0xFFFFE1EE; c.sleeve = 0xFFFFE1EE; c.belly = 0xFFFFFFFF; c.pants = 0xFFF7A6C9;
        c.shoeL = 0xFFFF86B6; c.shoeR = 0xFFFF86B6; c.tail = "bunny"; c.tailC = 0xFFFFFFFF;
        c.say = new String[]{"Hop hop!", "Cenoura?", "Oi!"};

        Skin d = build("panda", "Pandinha");
        d.hood = 0xFFFFFFFF; d.trim = 0xFF2C2C36; d.inner = 0xFF4A4A58; d.ears = "bear"; d.earC = 0xFF2C2C36;
        d.jacket = 0xFFFFFFFF; d.sleeve = 0xFF2C2C36; d.pants = 0xFF2C2C36; d.shoeL = 0xFF7ED957; d.shoeR = 0xFF7ED957;
        d.patch = true; d.say = new String[]{"Bambu!", "Oi!", "Abraço?"};

        Skin e = build("raposinha", "Raposinha");
        e.hood = 0xFFFF7A33; e.trim = 0xFFFFF4EA; e.inner = 0xFFFFF4EA; e.ears = "fox";
        e.jacket = 0xFFFF7A33; e.sleeve = 0xFFFF7A33; e.belly = 0xFFFFF4EA; e.pants = 0xFF6B3D24;
        e.shoeL = 0xFF34303F; e.shoeR = 0xFF34303F; e.tail = "fox"; e.tailC = 0xFFFF7A33;
        e.say = new String[]{"Hihi!", "Oi!", "Sou espertinha!"};

        Skin f = build("fadinha", "Fadinha");
        f.hasHood = false; f.trim = 0xFFFFD36B; f.dress = true; f.dressC = 0xFFC9A2FF; f.jacket = 0xFFC9A2FF;
        f.sleeve = 0xFFE6D4FF; f.pants = 0xFFE6D4FF; f.shoeL = 0xFFFFD36B; f.shoeR = 0xFFFFD36B;
        f.wings = true; f.crown = true; f.prop = "wand"; f.fly = true;
        f.say = new String[]{"Pó de fada!", "Brilha, brilha!", "Oi!"};

        Skin g = build("unicornio", "Unicórnio");
        g.hood = 0xFFFBF6FF; g.trim = 0xFFFFCF5A; g.inner = 0xFFFFC3DF; g.ears = "uni"; g.horn = true; g.mane = true;
        g.jacket = 0xFFE7D7FF; g.sleeve = 0xFFE7D7FF; g.pants = 0xFFBFA3FF; g.shoeL = 0xFFFF9ED1; g.shoeR = 0xFF9FD8FF;
        g.tail = "uni"; g.rainbow = true; g.say = new String[]{"Arco-íris!", "Brilho!", "Oi!"};

        Skin h = build("dino", "Dinossaurinha");
        h.hood = 0xFF5FCF6D; h.trim = 0xFFFFD84D; h.inner = 0xFFFFD84D; h.ears = "dino";
        h.jacket = 0xFF5FCF6D; h.sleeve = 0xFF5FCF6D; h.belly = 0xFFEAFFC9; h.pants = 0xFF3F9A4B;
        h.shoeL = 0xFFFFD84D; h.shoeR = 0xFFFFD84D; h.tail = "dino"; h.tailC = 0xFF5FCF6D;
        h.say = new String[]{"Rawr!", "Grrr... hihi!", "Oi!"};

        return new Skin[]{a, b, c, d, e, f, g, h};
    }
}
