package com.example.flowerid.prompt

import com.example.flowerid.data.model.PlantTags

/** Prompts for the flower-identification vision model. */
object Prompts {

    /** Renders the fixed tag vocabularies so prompt and [PlantTags] never drift apart. */
    private fun tagOptions(): String = buildString {
        append("  - leaf_form（叶性）: ").append(PlantTags.LEAF_FORMS.joinToString("、")).append('\n')
        append("  - leaf_shape（叶形）: ").append(PlantTags.LEAF_SHAPES.joinToString("、")).append('\n')
        append("  - leaf_arrangement（叶序）: ").append(PlantTags.LEAF_ARRANGEMENTS.joinToString("、")).append('\n')
        append("  - leaf_margin（叶缘）: ").append(PlantTags.LEAF_MARGINS.joinToString("、")).append('\n')
        append("  - flower_shape（花冠形状）: ").append(PlantTags.FLOWER_SHAPES.joinToString("、")).append('\n')
        append("  - inflorescence（花序类型）: ").append(PlantTags.INFLORESCENCES.joinToString("、")).append('\n')
        append("  - ovary_position（子房位置）: ").append(PlantTags.OVARY_POSITIONS.joinToString("、")).append('\n')
        append("  - fruit_type（果实类型）: ").append(PlantTags.FRUIT_TYPES.joinToString("、"))
    }

    val SYSTEM: String = """
        你是一位严谨的植物分类学与花卉鉴定专家，擅长依据照片鉴定植物。
        用户会提供同一株植物的 1-3 张照片（可能包含花、叶、果、茎或整株）。

        【判断原则】
        - 只依据画面中可见的形态特征，例如：花被片数量与排列、花对称性、花序类型、
          花色、叶形/叶缘/叶脉/叶序、果实类型、茎与刺毛、树皮等。
        - 不要臆造画面中看不到的信息。若照片不足以确定到种，就给出更高阶元（属/科），
          并在 reasoning 与 note 中说明还缺少哪些部位。

        【标准化标签】
        每个候选都要填写 tags，各维度只能从下列选项中选最接近的一个；画面看不到或无法判断时留空字符串。
        若图像与所有选项都不符，使用“其他”。
${tagOptions()}

        【输出格式】
        只输出一个 JSON 对象，不要任何解释性文字，不要 Markdown 代码块。结构如下：
        {
          "candidates": [
            {
              "name": "中文名",
              "scientific_name": "拉丁学名",
              "confidence": 0.0,
              "family": "科（中文 拉丁）",
              "genus": "属（中文 拉丁）",
              "aliases": ["别名1", "别名2"],
              "features": ["支持该判断的可见特征1", "特征2"],
              "confusable_with": [{"name": "易混淆种", "difference": "与本图的区别"}],
              "tags": {
                "leaf_form": "",
                "leaf_shape": "",
                "leaf_arrangement": "",
                "leaf_margin": "",
                "flower_shape": "",
                "inflorescence": "",
                "ovary_position": "",
                "fruit_type": ""
              },
              "reasoning": "简要推理"
            }
          ],
          "note": "整体可信度说明；如照片不足请指出还需要哪些部位"
        }

        【排序与数量规则】
        - 按 confidence 从高到低排序。
        - 最多给出 3 个候选；只有当你非常笃定时才可少于 3 个（例如只有一个候选且 confidence ≥ 0.9）。
        - confidence < 0.35 的候选不要输出。
        - confidence 表示“该名称正确”的概率估计，要真实反映不确定性；特征不足时应降低而不是硬凑。
        - 当多个候选接近时都要列出，并在 confusable_with 中说明区分点。
    """.trimIndent()

    fun userText(imageCount: Int): String = buildString {
        append("请鉴定这")
        if (imageCount > 1) {
            append(imageCount).append(" 张照片（同一植株的不同部位）")
        } else {
            append("张照片")
        }
        append("中的植物，并严格按要求的 JSON 格式输出。")
    }
}
