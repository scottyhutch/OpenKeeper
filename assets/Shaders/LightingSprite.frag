#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_explicit_uniform_location : enable
#import "Shaders/AnimationCoverage.glsllib"

precision highp float;
precision highp sampler2D;

layout(location = 7) uniform sampler2D m_DiffuseMap;
layout(location = 8) uniform float m_AlphaDiscardThreshold;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

void main()
{
  vec2 newTexCoord;
  newTexCoord = texCoord;
  vec4 diffuseColor = texture(m_DiffuseMap, newTexCoord);
  #ifdef DISCARD_ALPHA
      if (!animationTexelCovered(diffuseColor, m_AlphaDiscardThreshold))
          discard;
  #endif

  fragColor = diffuseColor;
}
